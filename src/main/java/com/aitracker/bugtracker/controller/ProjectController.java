package com.aitracker.bugtracker.controller;

import com.aitracker.bugtracker.entity.Project;
import com.aitracker.bugtracker.entity.ProjectType;
import com.aitracker.bugtracker.service.ProjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.aitracker.bugtracker.service.BugService;
import com.aitracker.bugtracker.service.ExternalTestingService;
import com.aitracker.bugtracker.entity.ProjectMemberRole;
import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.service.ProjectMemberService;
import com.aitracker.bugtracker.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import com.aitracker.bugtracker.entity.Role;
import org.springframework.security.access.AccessDeniedException;

@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final BugService bugService;
    private final ExternalTestingService externalTestingService;
    private final ProjectMemberService projectMemberService;
    private final UserService userService;

    public ProjectController(
            ProjectService projectService,
            BugService bugService,
            ExternalTestingService externalTestingService,
            ProjectMemberService projectMemberService,
            UserService userService) {

        this.projectService = projectService;
        this.bugService = bugService;
        this.externalTestingService = externalTestingService;
        this.projectMemberService = projectMemberService;
        this.userService = userService;
    }

    // Project list page
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'TESTER')")
    public String projectList(Model model, Authentication authentication) {

        User currentUser = userService.getUserByEmail(authentication.getName());

        if (currentUser.getRole() == Role.ADMIN) {
            model.addAttribute("projects", projectService.getAllProjects());
        } else {
            model.addAttribute("projects", projectService.getProjectsForUser(currentUser));
        }

        return "projects";
    }

    // Create project page
    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public String showCreateProjectPage(Model model) {

        model.addAttribute("projectTypes", ProjectType.values());

        return "project-form";
    }
    // Edit project page
    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public String showEditProjectPage(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        requireProjectManagerAccess(id, currentUser);

        Project project = projectService.getProjectById(id);

        model.addAttribute("project", project);
        model.addAttribute("projectTypes", ProjectType.values());
        model.addAttribute(
                "projectStatuses",
                com.aitracker.bugtracker.entity.ProjectStatus.values()
        );

        return "project-edit";
    }
    // Project details page
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'TESTER')")
    public String projectDetails(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        requireProjectAccess(id, currentUser);

        Project project = projectService.getProjectById(id);

        model.addAttribute("project", project);
        model.addAttribute("bugs", bugService.getProjectBugs(id));
        model.addAttribute("members", projectMemberService.getProjectMembers(id));
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("memberRoles", ProjectMemberRole.values());

        return "project-details";
    }
    // Add team member
    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public String addMember(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam ProjectMemberRole memberRole,
            Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        requireProjectManagerAccess(id, currentUser);

        projectMemberService.addMember(id, userId, memberRole);

        return "redirect:/projects/" + id;
    }


    // Remove team member
    @PostMapping("/{id}/members/remove")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public String removeMember(
            @PathVariable Long id,
            @RequestParam Long memberId,
            Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        requireProjectManagerAccess(id, currentUser);

        boolean memberBelongsToProject =
                projectMemberService.getProjectMembers(id)
                        .stream()
                        .anyMatch(member ->
                                member.getId().equals(memberId));

        if (!memberBelongsToProject) {
            throw new AccessDeniedException(
                    "This member does not belong to this project");
        }

        projectMemberService.removeMember(memberId);

        return "redirect:/projects/" + id;
    }
    // Test external project connectivity
    @PostMapping("/{id}/test-connectivity")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public String testConnectivity(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        requireProjectManagerAccess(id, currentUser);

        Project project = projectService.getProjectById(id);

        String result =
                externalTestingService.checkConnectivity(project);

        model.addAttribute("project", project);
        model.addAttribute("bugs", bugService.getProjectBugs(id));
        model.addAttribute(
                "members", projectMemberService.getProjectMembers(id));
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("memberRoles", ProjectMemberRole.values());
        model.addAttribute("testResult", result);

        return "project-details";
    }
    // Create project
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public String createProject(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam ProjectType projectType,
            @RequestParam(required = false) String projectUrl,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        if (projectType == ProjectType.EXTERNAL &&
                (projectUrl == null || projectUrl.isBlank())) {

            return "redirect:/projects/new?error=externalUrlRequired";
        }

        projectService.createProject(
                name,
                description,
                projectType,
                projectUrl,
                startDate,
                endDate
        );

        return "redirect:/projects";
    }
    // Update project
    @PostMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public String updateProject(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam ProjectType projectType,
            @RequestParam(required = false) String projectUrl,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam com.aitracker.bugtracker.entity.ProjectStatus status,
            Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        requireProjectManagerAccess(id, currentUser);

        projectService.updateProject(
                id,
                name,
                description,
                projectType,
                projectUrl,
                startDate,
                endDate,
                status
        );

        return "redirect:/projects";
    }
    // Delete project
    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public String deleteProject(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        requireProjectManagerAccess(id, currentUser);

        projectService.deleteProject(id);

        return "redirect:/projects";
    }
    private void requireProjectAccess(Long projectId, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        boolean assigned = projectMemberService
                .getProjectMembers(projectId)
                .stream()
                .anyMatch(member ->
                        member.getUser().getId().equals(currentUser.getId())
                                && (currentUser.getRole() != Role.PROJECT_MANAGER
                                || member.getMemberRole()
                                == ProjectMemberRole.PROJECT_MANAGER));

        if (!assigned) {
            throw new AccessDeniedException(
                    "You are not assigned to this project");
        }
    }

    private void requireProjectManagerAccess(
            Long projectId, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() != Role.PROJECT_MANAGER) {
            throw new AccessDeniedException(
                    "Only Admin or assigned Project Manager can manage this project");
        }

        boolean assigned = projectMemberService
                .getProjectMembers(projectId)
                .stream()
                .anyMatch(member ->
                        member.getUser().getId().equals(currentUser.getId())
                                && member.getMemberRole()
                                == ProjectMemberRole.PROJECT_MANAGER);

        if (!assigned) {
            throw new AccessDeniedException(
                    "You are not assigned to this project");
        }
    }
}