package org.saeta.digitalidentitysystem.controller;

import org.saeta.digitalidentitysystem.dto.NotificationDTO;
import org.saeta.digitalidentitysystem.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/user/{userId}/unread")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(@PathVariable Long userId) {
        List<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId);
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public ResponseEntity<Map<String, Object>> getAllNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationDTO> notificationsPage = notificationService.getAllNotifications(userId, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notificationsPage.getContent());
        response.put("currentPage", notificationsPage.getNumber());
        response.put("totalItems", notificationsPage.getTotalElements());
        response.put("totalPages", notificationsPage.getTotalPages());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}/count")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public ResponseEntity<Integer> getUnreadCount(@PathVariable Long userId) {
        int count = notificationService.getUnreadCount(userId);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/user/{userId}/read-all")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createNotification(
            @PathVariable Long userId,
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam(defaultValue = "INFO") String notificationType) {

        notificationService.createNotification(userId, title, message, notificationType);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
