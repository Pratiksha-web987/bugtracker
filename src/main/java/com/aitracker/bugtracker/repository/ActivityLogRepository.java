package com.aitracker.bugtracker.repository;

import com.aitracker.bugtracker.entity.ActivityLog;
import com.aitracker.bugtracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByOrderByCreatedAtDesc();

    List<ActivityLog> findByUserOrderByCreatedAtDesc(User user);
}