package com.aitracker.bugtracker.repository;

import com.aitracker.bugtracker.entity.Bug;
import com.aitracker.bugtracker.entity.BugStatus;
import com.aitracker.bugtracker.entity.Project;
import com.aitracker.bugtracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface BugRepository extends JpaRepository<Bug, Long> {

    List<Bug> findByProject(Project project);

    List<Bug> findByReportedBy(User user);

    List<Bug> findByAssignedTo(User user);

    List<Bug> findByProjectIn(List<Project> projects);

    List<Bug> findByStatus(BugStatus status);

    long countByProject(Project project);

    long countByStatus(BugStatus status);

    boolean existsByProjectAndTitle(Project project, String title);
}