package com.aitracker.bugtracker.controller;

import com.aitracker.bugtracker.entity.BugStatus;
import com.aitracker.bugtracker.entity.Role;
import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.service.BugService;
import com.aitracker.bugtracker.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.aitracker.bugtracker.service.ActivityLogService;

@Controller
public class AuthController {

    private final UserService userService;
    private final BugService bugService;
    private final ActivityLogService activityLogService;

    public AuthController(
            UserService userService,
            BugService bugService,
            ActivityLogService activityLogService) {

        this.userService = userService;
        this.bugService = bugService;
        this.activityLogService = activityLogService;
    }

    // Register page
    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    // Register form submit
    @PostMapping("/register")
    public String registerUser(
            @RequestParam Long id,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {

        if (!password.equals(confirmPassword)) {
            model.addAttribute(
                    "error",
                    "Passwords do not match!"
            );

            return "register";
        }

        try {

            userService.registerUser(
                    id,
                    name,
                    email,
                    password,
                    Role.TESTER
            );

            return "redirect:/login?registered";

        } catch (RuntimeException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "register";
        }
    }

    // Login page
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "403";
    }

    // Dashboard
    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        String email = authentication.getName();

        User user =
                userService.getUserByEmail(email);

        model.addAttribute("user", user);

        // Get only bugs visible to the current user
        var userBugs = bugService.getBugsForUser(user);

        model.addAttribute(
                "totalBugs",
                userBugs.size()
        );

        model.addAttribute(
                "openBugs",
                userBugs.stream()
                        .filter(b -> b.getStatus() == BugStatus.OPEN)
                        .count()
        );

        model.addAttribute(
                "assignedBugs",
                userBugs.stream()
                        .filter(b -> b.getStatus() == BugStatus.ASSIGNED)
                        .count()
        );

        model.addAttribute(
                "inProgressBugs",
                userBugs.stream()
                        .filter(b -> b.getStatus() == BugStatus.IN_PROGRESS)
                        .count()
        );

        model.addAttribute(
                "fixedBugs",
                userBugs.stream()
                        .filter(b -> b.getStatus() == BugStatus.FIXED)
                        .count()
        );

        model.addAttribute(
                "retestingBugs",
                userBugs.stream()
                        .filter(b -> b.getStatus() == BugStatus.RETESTING)
                        .count()
        );

        model.addAttribute(
                "closedBugs",
                userBugs.stream()
                        .filter(b -> b.getStatus() == BugStatus.CLOSED)
                        .count()
        );

        model.addAttribute(
                "reopenedBugs",
                userBugs.stream()
                        .filter(b -> b.getStatus() == BugStatus.REOPENED)
                        .count()
        );

        return "dashboard";
    }
}