package com.aitracker.bugtracker.service;

import com.aitracker.bugtracker.entity.ActivityLog;
import com.aitracker.bugtracker.entity.Bug;
import com.aitracker.bugtracker.entity.ProjectMember;
import com.aitracker.bugtracker.entity.Role;
import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.repository.ActivityLogRepository;
import com.aitracker.bugtracker.repository.BugCommentRepository;
import com.aitracker.bugtracker.repository.BugRepository;
import com.aitracker.bugtracker.repository.ProjectMemberRepository;
import com.aitracker.bugtracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aitracker.bugtracker.service.NotificationService;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectMemberRepository projectMemberRepository;
    private final BugRepository bugRepository;
    private final BugCommentRepository bugCommentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationService notificationService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ProjectMemberRepository projectMemberRepository,
            BugRepository bugRepository,
            BugCommentRepository bugCommentRepository,
            ActivityLogRepository activityLogRepository,
            NotificationService notificationService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.projectMemberRepository = projectMemberRepository;
        this.bugRepository = bugRepository;
        this.bugCommentRepository = bugCommentRepository;
        this.activityLogRepository = activityLogRepository;
        this.notificationService = notificationService;
    }

    public User registerUser(
            Long id,
            String name,
            String email,
            String password,
            Role role) {

        if (id == null || id <= 0) {
            throw new RuntimeException("Please enter a valid User ID");
        }

        if (userRepository.existsById(id)) {
            throw new RuntimeException("User ID already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);

        return userRepository.save(user);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateUserRole(Long id, Role role) {
        User user = getUserById(id);
        user.setRole(role);
        return userRepository.save(user);
    }

    public User updateUserDetails(
            Long id,
            String name,
            String email,
            Role role) {

        User user = getUserById(id);

        // Check whether email is being changed
        if (!user.getEmail().equals(email)
                && userRepository.existsByEmail(email)) {

            throw new RuntimeException(
                    "Email already registered"
            );
        }

        user.setName(name);
        user.setEmail(email);
        user.setRole(role);

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id, Long currentUserId) {

        // 1. Prevent deleting currently logged-in user
        if (id.equals(currentUserId)) {
            throw new RuntimeException(
                    "You cannot delete your own account"
            );
        }

        // 2. Check target user exists
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // 3. Do not silently destroy bugs reported by this user
        List<Bug> reportedBugs = bugRepository.findByReportedBy(user);

        if (!reportedBugs.isEmpty()) {
            throw new RuntimeException(
                    "Cannot delete this user because they have "
                            + reportedBugs.size()
                            + " reported bug(s). Reassign those bugs first."
            );
        }

        // 4. Remove project memberships
        List<ProjectMember> memberships =
                projectMemberRepository.findByUser(user);

        if (!memberships.isEmpty()) {
            projectMemberRepository.deleteAll(memberships);
        }

        // 5. Remove bug comments written by this user
        List<com.aitracker.bugtracker.entity.BugComment> comments =
                bugCommentRepository.findByUser(user);

        if (!comments.isEmpty()) {
            bugCommentRepository.deleteAll(comments);
        }

        // 6. Unassign bugs assigned to this user
        List<Bug> assignedBugs =
                bugRepository.findByAssignedTo(user);

        if (!assignedBugs.isEmpty()) {

            for (Bug bug : assignedBugs) {
                bug.setAssignedTo(null);
            }

            bugRepository.saveAll(assignedBugs);
        }

        // 7. Preserve activity logs but remove user reference
        List<ActivityLog> logs =
                activityLogRepository.findByUserOrderByCreatedAtDesc(user);

        if (!logs.isEmpty()) {

            for (ActivityLog log : logs) {
                log.setUser(null);
            }

            activityLogRepository.saveAll(logs);
        }
        // 8. Finally delete the user
        notificationService.deleteUserNotifications(user);
        userRepository.delete(user);

    }
}