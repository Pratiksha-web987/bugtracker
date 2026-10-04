package com.aitracker.bugtracker.controller;

import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.service.NotificationService;
import com.aitracker.bugtracker.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(
            NotificationService notificationService,
            UserService userService) {

        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public String notifications(
            Model model,
            org.springframework.security.core.Authentication authentication) {

        User user =
                userService.getUserByEmail(authentication.getName());

        model.addAttribute(
                "notifications",
                notificationService.getUserNotifications(user)
        );

        model.addAttribute(
                "unreadCount",
                notificationService.getUnreadCount(user)
        );

        return "notifications";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication) {

        User user =
                userService.getUserByEmail(authentication.getName());

        notificationService.markAsRead(id, user);

        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(
            org.springframework.security.core.Authentication authentication) {

        User user =
                userService.getUserByEmail(authentication.getName());

        notificationService.markAllAsRead(user);

        return "redirect:/notifications";
    }
}