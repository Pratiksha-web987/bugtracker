package com.aitracker.bugtracker.controller;

import com.aitracker.bugtracker.service.ActivityLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ActivityLogService activityLogService;

    public AdminController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {

        model.addAttribute(
                "logs",
                activityLogService.getAllLogs()
        );

        return "admin";
    }
}