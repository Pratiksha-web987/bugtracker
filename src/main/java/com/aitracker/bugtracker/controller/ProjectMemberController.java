package com.aitracker.bugtracker.controller;

import com.aitracker.bugtracker.entity.ProjectMemberRole;
import com.aitracker.bugtracker.repository.ProjectRepository;
import com.aitracker.bugtracker.repository.UserRepository;
import com.aitracker.bugtracker.service.ProjectMemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/projects/{projectId}/team")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectMemberController(
            ProjectMemberService projectMemberService,
            ProjectRepository projectRepository,
            UserRepository userRepository) {

        this.projectMemberService = projectMemberService;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    // Team page
    @GetMapping
    public String teamPage(
            @PathVariable Long projectId,
            Model model) {

        var project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        model.addAttribute("project", project);
        model.addAttribute(
                "members",
                projectMemberService.getProjectMembers(projectId)
        );
        model.addAttribute(
                "users",
                userRepository.findAll()
        );
        model.addAttribute(
                "memberRoles",
                ProjectMemberRole.values()
        );

        return "project-team";
    }

    // Add member
    @PostMapping("/add")
    public String addMember(
            @PathVariable Long projectId,
            @RequestParam Long userId,
            @RequestParam ProjectMemberRole memberRole) {

        projectMemberService.addMember(
                projectId,
                userId,
                memberRole
        );

        return "redirect:/projects/" + projectId + "/team";
    }

    // Remove member
    @PostMapping("/remove/{memberId}")
    public String removeMember(
            @PathVariable Long projectId,
            @PathVariable Long memberId) {

        projectMemberService.removeMember(memberId);

        return "redirect:/projects/" + projectId + "/team";
    }
}