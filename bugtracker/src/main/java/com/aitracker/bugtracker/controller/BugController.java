package com.aitracker.bugtracker.controller;

import com.aitracker.bugtracker.entity.BugPriority;
import com.aitracker.bugtracker.entity.BugSeverity;
import com.aitracker.bugtracker.entity.Project;
import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.repository.ProjectRepository;
import com.aitracker.bugtracker.repository.UserRepository;
import com.aitracker.bugtracker.service.BugService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.aitracker.bugtracker.entity.BugStatus;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import com.aitracker.bugtracker.service.UserService;
import org.springframework.security.core.Authentication;
import com.aitracker.bugtracker.service.BugCommentService;
import com.aitracker.bugtracker.service.AIService;
import com.aitracker.bugtracker.entity.Bug;

@Controller
@RequestMapping("/bugs")
public class BugController {

    private final BugService bugService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final BugCommentService bugCommentService;
    private final AIService aiService;

    public BugController(
            BugService bugService,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            UserService userService,
            BugCommentService bugCommentService,
            AIService aiService) {

        this.bugService = bugService;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.bugCommentService = bugCommentService;
        this.aiService = aiService;
    }

    // All bugs
    @GetMapping
    public String listBugs(Model model, Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        model.addAttribute(
                "bugs",
                bugService.getBugsForUser(currentUser)
        );

        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("statuses", BugStatus.values());
        model.addAttribute("priorities", BugPriority.values());
        model.addAttribute("severities", BugSeverity.values());

        // Valid status transitions for every bug
        Map<Long, List<BugStatus>> validStatuses = new HashMap<>();

        List<Bug> bugs = bugService.getBugsForUser(currentUser);

        for (Bug bug : bugs) {
            validStatuses.put(
                    bug.getId(),
                    Arrays.asList(BugStatus.values())
            );
        }

        model.addAttribute("validStatuses", validStatuses);

        return "bugs";
    }
    // Bug details
    @GetMapping("/{id}")
    public String bugDetails(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "bug",
                bugService.getBugById(id)
        );

        model.addAttribute(
                "users",
                userRepository.findAll()
        );

        model.addAttribute(
                "statuses",
                com.aitracker.bugtracker.entity.BugStatus.values()
        );
        model.addAttribute(
                "comments",
                bugCommentService.getCommentsByBug(id)
        );
        return "bug-details";
    }

    // Create bug page
    @GetMapping("/new")
    public String showCreateBugPage(Model model) {

        model.addAttribute(
                "projects",
                projectRepository.findAll()
        );

        model.addAttribute(
                "users",
                userRepository.findAll()
        );

        model.addAttribute(
                "priorities",
                BugPriority.values()
        );

        model.addAttribute(
                "severities",
                BugSeverity.values()
        );

        return "bug-form";
    }
    @PostMapping("/{id}/comments")
    public String addComment(
            @PathVariable Long id,
            @RequestParam String comment,
            Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        bugCommentService.addComment(
                id,
                currentUser,
                comment
        );

        return "redirect:/bugs/" + id;
    }
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{bugId}/comments/delete/{commentId}")
    public String deleteComment(
            @PathVariable Long bugId,
            @PathVariable Long commentId,
            Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        bugCommentService.deleteComment(
                commentId,
                currentUser
        );

        return "redirect:/bugs/" + bugId;
    }
    // Create bug
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'TESTER')")
    @PostMapping
    public String createBug(
            @RequestParam Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) String stepsToReproduce,
            @RequestParam(required = false) String expectedResult,
            @RequestParam(required = false) String actualResult,
            @RequestParam Long projectId,
            @RequestParam Long reportedById,
            @RequestParam BugPriority priority,
            @RequestParam BugSeverity severity,
            @RequestParam(required = false) String category) {


        bugService.createBug(
                id,
                title,
                description,
                stepsToReproduce,
                expectedResult,
                actualResult,
                projectId,
                reportedById,
                priority,
                severity,
                category
        );

        bugService.autoTriageBug(id);

        return "redirect:/bugs";
    }
    // Edit bug page
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'TESTER')")
    @GetMapping("/{id}/edit")
    public String showEditBugPage(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "bug",
                bugService.getBugById(id)
        );

        model.addAttribute(
                "priorities",
                BugPriority.values()
        );

        model.addAttribute(
                "severities",
                BugSeverity.values()
        );

        return "bug-edit";
    }
    // Update bug
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'TESTER')")
    @PostMapping("/{id}/update")
    public String updateBug(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) String stepsToReproduce,
            @RequestParam(required = false) String expectedResult,
            @RequestParam(required = false) String actualResult,
            @RequestParam BugPriority priority,
            @RequestParam BugSeverity severity,
            @RequestParam(required = false) String category) {

        bugService.updateBug(
                id,
                title,
                description,
                stepsToReproduce,
                expectedResult,
                actualResult,
                priority,
                severity,
                category
        );

        return "redirect:/bugs/" + id;
    }
    // Assign bug to user
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @PostMapping("/{id}/assign")
    public String assignBug(
            @PathVariable Long id,
            @RequestParam Long userId) {

        bugService.assignBug(id, userId);

        return "redirect:/bugs";
    }
    // Triage bug
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @PostMapping("/{id}/triage")
    public String triageBug(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        bugService.updateStatus(
                id,
                BugStatus.TRIAGED,
                currentUser
        );

        return "redirect:/bugs";
    }

    // Update bug status
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'TESTER')")
    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam BugStatus status,
            Authentication authentication) {

        User currentUser =
                userService.getUserByEmail(authentication.getName());

        bugService.updateStatus(
                id,
                status,
                currentUser
        );

        return "redirect:/bugs";
    }

    // Delete bug
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @PostMapping("/delete/{id}")
    public String deleteBug(@PathVariable Long id) {

        bugService.deleteBug(id);

        return "redirect:/bugs";
    }
    @GetMapping("/{id}/ai-analysis")
    @ResponseBody
    public String analyzeBugWithAI(@PathVariable Long id) {

        Bug bug = bugService.getBugById(id);

        return aiService.analyzeBug(
                bug.getTitle(),
                bug.getDescription()
        );
    }
    @GetMapping("/{id}/duplicate-check")
    @ResponseBody
    public String checkDuplicateBug(@PathVariable Long id) {

        Bug newBug = bugService.getBugById(id);

        List<Bug> existingBugs = bugService.getAllBugs()
                .stream()
                .filter(bug -> !bug.getId().equals(id))
                .toList();

        return aiService.detectDuplicateBug(
                newBug.getTitle(),
                newBug.getDescription(),
                existingBugs
        );
    }
    @GetMapping("/{id}/root-cause")
    @ResponseBody
    public String analyzeRootCause(@PathVariable Long id) {

        Bug bug = bugService.getBugById(id);

        return aiService.analyzeRootCause(
                bug.getTitle(),
                bug.getDescription()
        );
    }
    @GetMapping("/{id}/fix-suggestion")
    @ResponseBody
    public String suggestFix(@PathVariable Long id) {
        Bug bug = bugService.getBugById(id);

        return aiService.suggestFixAndDebuggingSteps(
                bug.getTitle(),
                bug.getDescription()
        );
    }
    @GetMapping("/{id}/developer-recommendation")
    @ResponseBody
    public String recommendDeveloper(@PathVariable Long id) {

        Bug bug = bugService.getBugById(id);

        List<User> developers = userRepository.findAll();

        return aiService.recommendDeveloper(
                bug.getTitle(),
                bug.getDescription(),
                developers
        );
    }

}