package org.saeta.digitalidentitysystem.service;

import org.saeta.digitalidentitysystem.entity.User;
import org.saeta.digitalidentitysystem.entity.UserStatus;
import org.saeta.digitalidentitysystem.repository.UserRepository;
import org.saeta.digitalidentitysystem.repository.UserStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StatusSchedulerService {

    private final UserRepository userRepository;
    private final UserStatusRepository statusRepository;
    private final NotificationService notificationService;

    @Autowired
    public StatusSchedulerService(UserRepository userRepository,
                                  UserStatusRepository statusRepository,
                                  NotificationService notificationService) {
        this.userRepository = userRepository;
        this.statusRepository = statusRepository;
        this.notificationService = notificationService;
    }

    /**
     * Cada medianoche: mover a EXPIRED a quienes ya vencieron y siguen activos.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireMemberships() {
        UserStatus active = statusRepository.findByName(UserStatus.ACTIVE).orElseThrow();
        UserStatus expired = statusRepository.findByName(UserStatus.EXPIRED).orElseThrow();

        LocalDateTime now = LocalDateTime.now();
        List<User> toExpire =
                userRepository.findByStatusAndMembershipExpiryBefore(active, now);

        toExpire.forEach(u -> {
            u.setStatus(expired);
            notificationService.sendStatusChangeNotification(
                    u.getId(), UserStatus.ACTIVE, UserStatus.EXPIRED);
        });
    }

    /**
     * Todos los lunes a las 00:05: aviso de membresías que vencerán en ≤ 30 días.
     */
    @Scheduled(cron = "0 5 0 ? * MON")
    @Transactional
    public void notifyUpcomingExpirations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in30 = now.plusDays(30);

        List<User> aboutToExpire =
                userRepository.findByMembershipExpiryBetween(now, in30);

        aboutToExpire.forEach(u -> {
            long days = java.time.Duration.between(now, u.getMembershipExpiry()).toDays();
            notificationService.sendExpiryNotification(u.getId(), (int) days);
        });
    }
}