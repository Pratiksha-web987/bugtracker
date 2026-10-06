package com.aitracker.bugtracker.service;

import com.aitracker.bugtracker.entity.Bug;
import com.aitracker.bugtracker.entity.BugPriority;
import com.aitracker.bugtracker.entity.BugSeverity;
import com.aitracker.bugtracker.entity.BugStatus;
import com.aitracker.bugtracker.entity.Project;
import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.repository.BugRepository;
import com.aitracker.bugtracker.repository.ProjectRepository;
import com.aitracker.bugtracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import com.aitracker.bugtracker.service.AIService;
import com.aitracker.bugtracker.service.NotificationService;
import com.aitracker.bugtracker.entity.ProjectMember;
import com.aitracker.bugtracker.entity.ProjectMemberRole;
import com.aitracker.bugtracker.repository.ProjectMemberRepository;

@Service
public class BugService {
    private boolean isValidTransition(
            BugStatus currentStatus,
            BugStatus newStatus) {

        if (currentStatus == newStatus) {
            return true;
        }

        return switch (currentStatus) {

            case OPEN ->
                    newStatus == BugStatus.TRIAGED;

            case TRIAGED ->
                    newStatus == BugStatus.ASSIGNED;

            case ASSIGNED ->
                    newStatus == BugStatus.IN_PROGRESS;

            case IN_PROGRESS ->
                    newStatus == BugStatus.FIXED;

            case FIXED ->
                    newStatus == BugStatus.RETESTING;

            case RETESTING ->
                    newStatus == BugStatus.CLOSED
                            || newStatus == BugStatus.REOPENED;

            case REOPENED ->
                    newStatus == BugStatus.IN_PROGRESS;

            case CLOSED ->
                    false;
            default ->
                    false;
        };
    }

    private final BugRepository bugRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final NotificationService notificationService;
    private final ProjectMemberRepository projectMemberRepository;

    public BugService(
            BugRepository bugRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            AIService aiService,
            NotificationService notificationService,
            ProjectMemberRepository projectMemberRepository) {

        this.bugRepository = bugRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.notificationService = notificationService;
        this.projectMemberRepository = projectMemberRepository;
    }
    // Create Bug
    public Bug createBug(
            Long id,
            String title,
            String description,
            String stepsToReproduce,
            String expectedResult,
            String actualResult,
            Long projectId,
            Long reportedById,
            BugPriority priority,
            BugSeverity severity,
            String category) {
        if (id == null || id <= 0) {
            throw new RuntimeException("Please enter a valid Bug ID");
        }

        if (bugRepository.existsById(id)) {
            throw new RuntimeException("Bug ID already exists");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        User reportedBy = userRepository.findById(reportedById)
                .orElseThrow(() ->
                        new RuntimeException("Reporter not found"));

        Bug bug = new Bug();
        bug.setId(id);
        bug.setTitle(title);
        bug.setDescription(description);
        bug.setStepsToReproduce(stepsToReproduce);
        bug.setExpectedResult(expectedResult);
        bug.setActualResult(actualResult);

        bug.setProject(project);
        bug.setReportedBy(reportedBy);

        bug.setPriority(priority);
        bug.setSeverity(severity);
        bug.setCategory(category);

        bug.setStatus(BugStatus.OPEN);

        return bugRepository.save(bug);
    }
    public Bug autoTriageBug(Long bugId) {

        Bug bug = getBugById(bugId);

        String result = aiService.autoTriageBug(
                bug.getTitle(),
                bug.getDescription()
        );

        String[] parts = result.split("\\|");

        if (parts.length != 3) {
            throw new RuntimeException("Invalid AI triage response");
        }

        bug.setCategory(parts[0]);
        bug.setSeverity(BugSeverity.valueOf(parts[1]));
        bug.setPriority(BugPriority.valueOf(parts[2]));

        bug.setStatus(BugStatus.TRIAGED);

        return bugRepository.save(bug);
    }

    // Get all bugs
    public List<Bug> getAllBugs() {
        return bugRepository.findAll();
    }
    public List<Bug> getBugsByProject(Project project) {
        return bugRepository.findByProject(project);
    }
    // Get bugs of a project
    public List<Bug> getProjectBugs(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        return bugRepository.findByProject(project);
    }

    // Get bug by ID
    public Bug getBugById(Long id) {

        return bugRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Bug not found"));
    }

    // Assign bug to developer
    public Bug assignBug(Long bugId, Long userId) {

        Bug bug = getBugById(bugId);

        if (bug.getStatus() != BugStatus.TRIAGED) {
            throw new RuntimeException(
                    "Bug must be TRIAGED before assigning"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (user.getRole() != com.aitracker.bugtracker.entity.Role.DEVELOPER) {
            throw new RuntimeException(
                    "Bug can only be assigned to a Developer"
            );
        }

        bug.setAssignedTo(user);
        bug.setStatus(BugStatus.ASSIGNED);

        Bug savedBug = bugRepository.save(bug);

        notificationService.createNotification(
                user,
                "Bug Assigned",
                "Bug #" + bug.getId()
                        + " has been assigned to you: "
                        + bug.getTitle()
        );

        return savedBug;
    }
    // Update bug status
    // Update bug status with workflow validation
    public Bug updateStatus(
            Long bugId,
            BugStatus newStatus,
            User currentUser) {

        Bug bug = getBugById(bugId);

        BugStatus currentStatus = bug.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new RuntimeException(
                    "Invalid status transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }

        switch (newStatus) {

            case TRIAGED:

                if (currentUser.getRole() != com.aitracker.bugtracker.entity.Role.ADMIN
                        && currentUser.getRole() != com.aitracker.bugtracker.entity.Role.PROJECT_MANAGER) {

                    throw new RuntimeException(
                            "Only Admin or Project Manager can triage bugs"
                    );
                }

                break;

            case IN_PROGRESS:
            case FIXED:
                if (currentUser.getRole() != com.aitracker.bugtracker.entity.Role.DEVELOPER) {
                    throw new RuntimeException(
                            "Only Developer can update bug to " + newStatus
                    );
                }

                if (bug.getAssignedTo() == null
                        || !bug.getAssignedTo().getId().equals(currentUser.getId())) {
                    throw new RuntimeException(
                            "Only the assigned Developer can update this bug"
                    );
                }

                break;

            case RETESTING:
            case CLOSED:
            case REOPENED:

                if (currentUser.getRole() != com.aitracker.bugtracker.entity.Role.TESTER) {

                    throw new RuntimeException(
                            "Only Tester can update bug to "
                                    + newStatus
                    );
                }

                break;

            case ASSIGNED:

                if (currentUser.getRole() != com.aitracker.bugtracker.entity.Role.ADMIN
                        && currentUser.getRole() != com.aitracker.bugtracker.entity.Role.PROJECT_MANAGER) {

                    throw new RuntimeException(
                            "Only Admin or Project Manager can assign bugs"
                    );
                }

                break;

            default:
                break;
        }

        bug.setStatus(newStatus);

        Bug savedBug = bugRepository.save(bug);

// Notify the bug reporter about important status changes
        if (bug.getReportedBy() != null
                && !bug.getReportedBy().getId().equals(currentUser.getId())) {

            notificationService.createNotification(
                    bug.getReportedBy(),
                    "Bug Status Updated",
                    "Bug #" + bug.getId()
                            + " (" + bug.getTitle() + ")"
                            + " status changed from "
                            + currentStatus
                            + " to "
                            + newStatus
            );
        }

// If a bug is reopened, notify the assigned developer
        if (newStatus == BugStatus.REOPENED
                && bug.getAssignedTo() != null
                && !bug.getAssignedTo().getId().equals(currentUser.getId())) {

            notificationService.createNotification(
                    bug.getAssignedTo(),
                    "Bug Reopened",
                    "Bug #" + bug.getId()
                            + " (" + bug.getTitle() + ")"
                            + " has been reopened and requires attention."
            );
        }

        return savedBug;
    }
    // Update Bug
    public Bug updateBug(
            Long id,
            String title,
            String description,
            String stepsToReproduce,
            String expectedResult,
            String actualResult,
            BugPriority priority,
            BugSeverity severity,
            String category) {

        Bug bug = getBugById(id);

        bug.setTitle(title);
        bug.setDescription(description);
        bug.setStepsToReproduce(stepsToReproduce);
        bug.setExpectedResult(expectedResult);
        bug.setActualResult(actualResult);
        bug.setPriority(priority);
        bug.setSeverity(severity);
        bug.setCategory(category);

        return bugRepository.save(bug);
    }
    public long getTotalBugs() {
        return bugRepository.count();
    }

    public long getBugsByStatus(BugStatus status) {
        return bugRepository.countByStatus(status);
    }
    // Delete bug
    public void deleteBug(Long bugId) {

        if (!bugRepository.existsById(bugId)) {
            throw new RuntimeException("Bug not found");
        }

        bugRepository.deleteById(bugId);
    }
    public List<Bug> getBugsForUser(User user) {

        if (user == null) {
            return List.of();
        }

        switch (user.getRole()) {

            case ADMIN:
            case PROJECT_MANAGER:
                return bugRepository.findAll();

            case DEVELOPER:
                return bugRepository.findByAssignedTo(user);

            case TESTER: {
                List<ProjectMember> memberships =
                        projectMemberRepository.findByUser(user);

                List<Project> testerProjects = memberships.stream()
                        .filter(member ->
                                member.getMemberRole() == ProjectMemberRole.TESTER)
                        .map(ProjectMember::getProject)
                        .toList();

                List<Bug> reportedBugs =
                        bugRepository.findByReportedBy(user);

                List<Bug> retestingBugs =
                        testerProjects.isEmpty()
                                ? List.of()
                                : bugRepository.findByProjectIn(testerProjects)
                                .stream()
                                .filter(bug ->
                                        bug.getStatus() == BugStatus.RETESTING)
                                .toList();

                return java.util.stream.Stream
                        .concat(reportedBugs.stream(), retestingBugs.stream())
                        .distinct()
                        .toList();
            }
            default:
                return List.of();
        }
    }
    public void createAutomatedBug(
            Long bugId,
            String title,
            String description,
            Long projectId,
            Long reportedById) {

        createBug(
                bugId,
                title,
                description,
                "Automatically detected by Selenium automated testing.",
                "Website should load successfully.",
                "Automated browser test detected a failure.",
                projectId,
                reportedById,
                BugPriority.HIGH,
                BugSeverity.MAJOR,
                "AUTOMATION"
        );

        autoTriageBug(bugId);
    }
}