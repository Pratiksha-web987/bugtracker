package com.aitracker.bugtracker.repository;

import com.aitracker.bugtracker.entity.Bug;
import com.aitracker.bugtracker.entity.BugComment;
import com.aitracker.bugtracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BugCommentRepository extends JpaRepository<BugComment, Long> {

    List<BugComment> findByBugOrderByCreatedAtAsc(Bug bug);

    List<BugComment> findByUser(User user);
}