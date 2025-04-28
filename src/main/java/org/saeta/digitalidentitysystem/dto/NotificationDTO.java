package org.saeta.digitalidentitysystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private String notificationType;
    private LocalDateTime createdAt;
    private Boolean isRead;
    private LocalDateTime readAt;
}
