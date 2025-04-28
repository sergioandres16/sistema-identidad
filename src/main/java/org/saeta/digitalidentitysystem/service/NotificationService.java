package org.saeta.digitalidentitysystem.service;

import org.saeta.digitalidentitysystem.dto.NotificationDTO;
import org.saeta.digitalidentitysystem.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    Notification createNotification(Long userId, String title, String message, String notificationType);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userId);

    List<NotificationDTO> getUnreadNotifications(Long userId);

    Page<NotificationDTO> getAllNotifications(Long userId, Pageable pageable);

    int getUnreadCount(Long userId);

    void sendAccessDeniedNotification(Long userId, Long accessLogId, String reason);

    void sendStatusChangeNotification(Long userId, String oldStatus, String newStatus);

    void sendDebtNotification(Long userId, String debtDetails);

    void sendExpiryNotification(Long userId, int daysLeft);
}
