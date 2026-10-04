package com.aitracker.bugtracker.service;

import com.aitracker.bugtracker.entity.Notification;
import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void createNotification(
            User user,
            String title,
            String message) {

        if (user == null) {
            return;
        }

        Notification notification =
                new Notification(user, title, message);

        notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(User user) {
        return notificationRepository
                .findByUserOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(User user) {
        return notificationRepository
                .countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"));

        // Security check:
        // User can only mark their own notification as read.
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You are not allowed to modify this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {

        List<Notification> notifications =
                notificationRepository
                        .findByUserOrderByCreatedAtDesc(user);

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void deleteUserNotifications(User user) {
        if (user == null) {
            return;
        }

        List<Notification> notifications =
                notificationRepository.findByUserOrderByCreatedAtDesc(user);

        if (!notifications.isEmpty()) {
            notificationRepository.deleteAll(notifications);
        }
    }
}