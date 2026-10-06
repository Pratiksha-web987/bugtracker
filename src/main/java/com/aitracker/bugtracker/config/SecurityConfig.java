package com.aitracker.bugtracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.service.ActivityLogService;
import com.aitracker.bugtracker.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserService userService,
            ActivityLogService activityLogService) throws Exception {

        AuthenticationSuccessHandler successHandler = (request, response, authentication) -> {

            String email = authentication.getName();

            User user = userService.getUserByEmail(email);

            if (user != null) {
                activityLogService.log(
                        user,
                        "LOGIN",
                        "User logged in successfully"
                );
            }

            response.sendRedirect("/dashboard");
        };

        http
                .authorizeHttpRequests(auth -> auth

                        // Public pages
                        .requestMatchers(
                                "/",
                                "/register",
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // Admin + Project Manager
                        .requestMatchers(
                                "/projects/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "PROJECT_MANAGER",
                                "DEVELOPER",
                                 "TESTER"
                        )

                        // Admin only
                        .requestMatchers(
                                "/users/**"
                        )
                        .hasRole("ADMIN")

                        // Admin + Project Manager + Developer + Tester
                        .requestMatchers(
                                "/bugs/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "PROJECT_MANAGER",
                                "DEVELOPER",
                                "TESTER"
                        )

                        // Dashboard for all logged-in users
                        .requestMatchers(
                                "/dashboard"
                        ).authenticated()

                        // Everything else
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/403")
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}