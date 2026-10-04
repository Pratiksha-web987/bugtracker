package com.aitracker.bugtracker.service;

import com.aitracker.bugtracker.entity.ActivityLog;
import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void log(User user, String action, String description) {
        ActivityLog activityLog =
                new ActivityLog(user, action, description);

        activityLogRepository.save(activityLog);
    }

    public List<ActivityLog> getAllLogs() {
        return activityLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<ActivityLog> getUserLogs(User user) {
        return activityLogRepository.findByUserOrderByCreatedAtDesc(user);
    }
}