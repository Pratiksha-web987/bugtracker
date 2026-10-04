package com.aitracker.bugtracker.repository;

import com.aitracker.bugtracker.entity.Project;
import com.aitracker.bugtracker.entity.ProjectMember;
import com.aitracker.bugtracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.aitracker.bugtracker.entity.ProjectMemberRole;

public interface ProjectMemberRepository
        extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProject(Project project);

    List<ProjectMember> findByUser(User user);

    Optional<ProjectMember> findByProjectAndUser(
            Project project,
            User user
    );

    boolean existsByProjectAndUser(
            Project project,
            User user
    );

    List<ProjectMember> findByProjectAndMemberRole(
            Project project,
            ProjectMemberRole memberRole
    );
}