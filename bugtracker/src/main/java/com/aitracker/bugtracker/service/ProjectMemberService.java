package com.aitracker.bugtracker.service;

import com.aitracker.bugtracker.entity.Project;
import com.aitracker.bugtracker.entity.ProjectMember;
import com.aitracker.bugtracker.entity.ProjectMemberRole;
import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.repository.ProjectMemberRepository;
import com.aitracker.bugtracker.repository.ProjectRepository;
import com.aitracker.bugtracker.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectMemberService(
            ProjectMemberRepository projectMemberRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository) {

        this.projectMemberRepository = projectMemberRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    // Add member to project
    public ProjectMember addMember(
            Long projectId,
            Long userId,
            ProjectMemberRole memberRole) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (projectMemberRepository.existsByProjectAndUser(
                project, user)) {

            throw new RuntimeException(
                    "User is already a member of this project");
        }

        ProjectMember member = new ProjectMember();

        member.setProject(project);
        member.setUser(user);
        member.setMemberRole(memberRole);

        return projectMemberRepository.save(member);
    }

    // Get all members of a project
    public List<ProjectMember> getProjectMembers(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        return projectMemberRepository.findByProject(project);
    }

    // Remove member
    public void removeMember(Long memberId) {

        if (!projectMemberRepository.existsById(memberId)) {
            throw new RuntimeException("Project member not found");
        }

        projectMemberRepository.deleteById(memberId);
    }
}