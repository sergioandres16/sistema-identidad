package org.saeta.digitalidentitysystem.service;

import org.saeta.digitalidentitysystem.dto.NotificationDTO;
import org.saeta.digitalidentitysystem.entity.Notification;
import org.saeta.digitalidentitysystem.entity.User;
import org.saeta.digitalidentitysystem.exception.ResourceNotFoundException;
import org.saeta.digitalidentitysystem.repository.NotificationRepository;
import org.saeta.digitalidentitysystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Autowired
    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Notification createNotification(Long userId, String title, String message, String notificationType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(notificationType);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setIsRead(false);

        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(now);
        });

        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        return unreadNotifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<NotificationDTO> getAllNotifications(Long userId, Pageable pageable) {
        Page<Notification> notificationsPage = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return notificationsPage.map(this::convertToDTO);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return (int) notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void sendAccessDeniedNotification(Long userId, Long accessLogId, String reason) {
        String title = "Acceso Denegado";
        String message = "Tu acceso ha sido denegado. Motivo: " + reason;
        createNotification(userId, title, message, "ACCESS");
    }

    @Override
    @Transactional
    public void sendStatusChangeNotification(Long userId, String oldStatus, String newStatus) {
        String title = "Estado Actualizado";
        String message = "Tu estado ha cambiado de " + (oldStatus != null ? oldStatus : "ninguno") +
                " a " + newStatus;
        createNotification(userId, title, message, "SYSTEM");
    }

    @Override
    @Transactional
    public void sendDebtNotification(Long userId, String debtDetails) {
        String title = "Deuda Pendiente";
        String message = "Tienes una deuda pendiente que afecta a tus privilegios de acceso: " + debtDetails;
        createNotification(userId, title, message, "DEBT");
    }

    @Override
    @Transactional
    public void sendExpiryNotification(Long userId, int daysLeft) {
        String title = "Membresía por Expirar";
        String message = "Tu membresía expirará en " + daysLeft + " días. Por favor renueva para mantener el acceso.";
        createNotification(userId, title, message, "INFO");
    }

    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUser().getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setNotificationType(notification.getNotificationType());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setIsRead(notification.getIsRead());
        dto.setReadAt(notification.getReadAt());
        return dto;
    }
}
