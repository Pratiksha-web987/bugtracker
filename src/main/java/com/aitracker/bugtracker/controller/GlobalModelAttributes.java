package com.aitracker.bugtracker.controller;

import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.service.NotificationService;
import com.aitracker.bugtracker.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserService userService;
    private final NotificationService notificationService;

    public GlobalModelAttributes(
            UserService userService,
            NotificationService notificationService) {

        this.userService = userService;
        this.notificationService = notificationService;
    }

    @ModelAttribute
    public void addGlobalAttributes(
            Model model,
            Authentication authentication) {

        long unreadCount = 0;

        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {

            User user =
                    userService.getUserByEmail(authentication.getName());

            if (user != null) {
                unreadCount =
                        notificationService.getUnreadCount(user);
            }
        }

        model.addAttribute(
                "unreadNotificationCount",
                unreadCount
        );
    }
}