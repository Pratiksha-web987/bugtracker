package com.aitracker.bugtracker.controller;

import com.aitracker.bugtracker.entity.Role;
import com.aitracker.bugtracker.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.aitracker.bugtracker.entity.User;
import org.springframework.security.access.prepost.PreAuthorize;


@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // User list
    @GetMapping
    public String userList(
            Model model,
            org.springframework.security.core.Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        model.addAttribute(
                "users",
                userService.getAllUsers()
        );

        model.addAttribute(
                "currentUser",
                currentUser
        );

        return "users";
    }

    // Update user role
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/role")
    public String updateRole(
            @PathVariable Long id,
            @RequestParam Role role) {

        userService.updateUserRole(id, role);

        return "redirect:/users";
    }

    // Open edit user page
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/edit/{id}")
    public String editUser(
            @PathVariable Long id,
            Model model) {

        User user = userService.getUserById(id);

        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());

        return "edit-user";
    }

    // Save edited user details
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/edit/{id}")
    public String updateUser(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam Role role,
            Model model) {

        try {

            userService.updateUserDetails(
                    id,
                    name,
                    email,
                    role
            );

            return "redirect:/users";

        } catch (RuntimeException e) {

            User user = userService.getUserById(id);

            model.addAttribute("user", user);
            model.addAttribute("roles", Role.values());
            model.addAttribute("error", e.getMessage());

            return "edit-user";
        }
    }

    // Delete user
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/delete/{id}")
    public String deleteUser(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication,
            Model model) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        try {

            userService.deleteUser(
                    id,
                    currentUser.getId()
            );

        } catch (RuntimeException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            model.addAttribute(
                    "users",
                    userService.getAllUsers()
            );

            return "users";
        }

        return "redirect:/users";

    }

}