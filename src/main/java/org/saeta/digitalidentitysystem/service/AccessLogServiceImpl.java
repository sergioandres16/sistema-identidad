package org.saeta.digitalidentitysystem.service;

import org.saeta.digitalidentitysystem.dto.AccessLogDTO;
import org.saeta.digitalidentitysystem.entity.AccessLog;
import org.saeta.digitalidentitysystem.entity.AccessZone;
import org.saeta.digitalidentitysystem.entity.User;
import org.saeta.digitalidentitysystem.entity.UserStatus;
import org.saeta.digitalidentitysystem.exception.ResourceNotFoundException;
import org.saeta.digitalidentitysystem.repository.AccessLogRepository;
import org.saeta.digitalidentitysystem.repository.AccessZoneRepository;
import org.saeta.digitalidentitysystem.repository.UserRepository;
import org.saeta.digitalidentitysystem.repository.UserStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccessLogServiceImpl implements AccessLogService {

    private final AccessLogRepository accessLogRepository;
    private final UserRepository userRepository;
    private final AccessZoneRepository accessZoneRepository;
    private final UserStatusRepository userStatusRepository;
    private final QrGeneratorService qrGeneratorService;
    private final NotificationService notificationService;

    @Autowired
    public AccessLogServiceImpl(
            AccessLogRepository accessLogRepository,
            UserRepository userRepository,
            AccessZoneRepository accessZoneRepository,
            UserStatusRepository userStatusRepository,
            QrGeneratorService qrGeneratorService,
            NotificationService notificationService) {
        this.accessLogRepository = accessLogRepository;
        this.userRepository = userRepository;
        this.accessZoneRepository = accessZoneRepository;
        this.userStatusRepository = userStatusRepository;
        this.qrGeneratorService = qrGeneratorService;
        this.notificationService = notificationService;
    }

    // ------------------------------------------------------------------------
    //  MÉTODO MODIFICADO: cambio de estado automático al escanear el QR
    // ------------------------------------------------------------------------
    @Override
    @Transactional
    public AccessLog processQrScan(String qrToken, Long zoneId,
                                   String scannerId, String scannerLocation) {

        // 1) Validar token → obtener userId
        Long userId = qrGeneratorService.validateQrToken(qrToken);
        if (userId == null) {
            return logAccess(null, zoneId, false, "SCAN",
                    scannerId, scannerLocation, "Invalid or expired QR code");
        }

        // 2) Cargar usuario y estado original
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id: " + userId));

        UserStatus originalStatus = user.getStatus();  // puede ser null
        String originalName = originalStatus != null ? originalStatus.getName() : null;

        // 3) Verificaciones de negocio
        boolean accessGranted = true;
        String reasonDenied = null;

        // 3.1) Deuda primero: si tiene deuda lo marcamos (y quizá negamos acceso)
        if (user.getHasDebt() != null && user.getHasDebt()
                && user.getMembershipType() != null && !user.getMembershipType().isEmpty()) {

            UserStatus debtStatus = userStatusRepository.findByName(UserStatus.DEBT)
                    .orElseThrow(() -> new ResourceNotFoundException("Status DEBT not found"));

            if (originalStatus == null || !debtStatus.getId().equals(originalStatus.getId())) {
                user.setStatus(debtStatus);
                userRepository.save(user);
                notificationService.sendStatusChangeNotification(
                        userId, originalName, UserStatus.DEBT);
            }
            accessGranted = false;
            reasonDenied = "User has outstanding debt";
        }

        // 3.2) Estado INACTIVE o SUSPENDED
        if (accessGranted && originalStatus != null) {
            String s = originalName;
            if (UserStatus.INACTIVE.equals(s) || UserStatus.SUSPENDED.equals(s)) {
                accessGranted = false;
                reasonDenied = "User status: " + s;
            }
        }

        // 3.3) Permisos de zona y franja horaria
        if (accessGranted && zoneId != null && user.getAccessProfile() != null) {
            boolean hasAccess = user.getAccessProfile().getAllowedZones().stream()
                    .anyMatch(z -> z.getId().equals(zoneId));

            if (!hasAccess) {
                accessGranted = false;
                reasonDenied = "No access rights to this zone";
            } else {
                LocalDateTime now = LocalDateTime.now();
                boolean timeAllowed = user.getAccessProfile().getTimeRestrictions().stream()
                        .anyMatch(tr -> tr.getDayOfWeek() == now.getDayOfWeek()
                                && tr.getStartTime().isBefore(now.toLocalTime())
                                && tr.getEndTime().isAfter(now.toLocalTime()));
                if (!timeAllowed) {
                    accessGranted = false;
                    reasonDenied = "Outside of allowed time period";
                }
            }
        }

        // 4) Reglas de promoción a ACTIVE si todo fue OK
        if (accessGranted) {
            // 4.1) PENDING -> ACTIVE
            if (UserStatus.PENDING.equals(originalName)) {
                promoteToActive(user, originalName);
            }
            // 4.2) EXPIRED -> ACTIVE si renovó
            else if (UserStatus.EXPIRED.equals(originalName)
                    && user.getMembershipExpiry() != null
                    && user.getMembershipExpiry().isAfter(LocalDateTime.now())) {
                promoteToActive(user, originalName);
            }
        }

        // 5) Registrar intento de acceso
        AccessLog accessLog = logAccess(userId, zoneId, accessGranted, "SCAN",
                scannerId, scannerLocation, reasonDenied);

        // 5.1) Si hubo cambio de estado, reflejarlo en el log
        if (user.getStatus() != null
                && (originalStatus == null
                || !user.getStatus().getId().equals(originalStatus.getId()))) {

            accessLog.setPreviousStatus(originalName);
            accessLog.setUpdatedStatus(user.getStatus().getName());
        }

        // 5.2) Notificar acceso denegado
        if (!accessGranted) {
            notificationService.sendAccessDeniedNotification(
                    userId, accessLog.getId(), reasonDenied);
        }

        return accessLogRepository.save(accessLog);
    }

    // ------------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------------
    private void promoteToActive(User user, String previousStatusName) {
        UserStatus activeStatus = userStatusRepository.findByName(UserStatus.ACTIVE)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Status ACTIVE not found"));
        user.setStatus(activeStatus);
        userRepository.save(user);
        notificationService.sendStatusChangeNotification(
                user.getId(), previousStatusName, UserStatus.ACTIVE);
    }

    @Override
    @Transactional
    public AccessLog logAccess(Long userId, Long zoneId, boolean accessGranted, String accessType,
                               String scannerId, String scannerLocation, String reasonDenied) {
        User user = userId != null ? userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId))
                : null;

        AccessZone zone = null;
        if (zoneId != null) {
            zone = accessZoneRepository.findById(zoneId)
                    .orElseThrow(() -> new ResourceNotFoundException("Access zone not found with id: " + zoneId));
        }

        AccessLog accessLog = new AccessLog();
        accessLog.setUser(user);
        accessLog.setZone(zone);
        accessLog.setAccessTime(LocalDateTime.now());
        accessLog.setAccessGranted(accessGranted);
        accessLog.setAccessType(accessType);
        accessLog.setScannerId(scannerId);
        accessLog.setScannerLocation(scannerLocation);
        accessLog.setReasonDenied(reasonDenied);

        if (user != null && user.getStatus() != null) {
            accessLog.setPreviousStatus(user.getStatus().getName());
        }

        return accessLogRepository.save(accessLog);
    }

    @Override
    @Transactional
    public AccessLog changeUserStatusOnAccess(Long userId, Long zoneId, Long newStatusId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        AccessZone zone = null;
        if (zoneId != null) {
            zone = accessZoneRepository.findById(zoneId)
                    .orElseThrow(() -> new ResourceNotFoundException("Access zone not found with id: " + zoneId));
        }

        UserStatus newStatus = userStatusRepository.findById(newStatusId)
                .orElseThrow(() -> new ResourceNotFoundException("User status not found with id: " + newStatusId));

        // Record previous status
        String previousStatus = user.getStatus() != null ? user.getStatus().getName() : null;

        // Change user status
        user.setStatus(newStatus);
        userRepository.save(user);

        // Log the status change
        AccessLog accessLog = new AccessLog();
        accessLog.setUser(user);
        accessLog.setZone(zone);
        accessLog.setAccessTime(LocalDateTime.now());
        accessLog.setAccessGranted(true);
        accessLog.setAccessType("STATUS_CHANGE");
        accessLog.setPreviousStatus(previousStatus);
        accessLog.setUpdatedStatus(newStatus.getName());

        accessLog = accessLogRepository.save(accessLog);

        // Send notification about status change
        notificationService.sendStatusChangeNotification(userId, previousStatus, newStatus.getName());

        return accessLog;
    }

    @Override
    public List<AccessLog> findByUserId(Long userId) {
        return accessLogRepository.findByUserIdOrderByAccessTimeDesc(userId);
    }

    @Override
    public List<AccessLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return accessLogRepository.findByAccessTimeBetween(startDate, endDate);
    }

    @Override
    public Page<AccessLog> findByUserId(Long userId, Pageable pageable) {
        return accessLogRepository.findByUserIdOrderByAccessTimeDesc(userId, pageable);
    }

    @Override
    public AccessLogDTO getLatestAccessForUser(Long userId) {
        List<AccessLog> logs = accessLogRepository.findByUserIdOrderByAccessTimeDesc(userId);

        if (logs.isEmpty()) {
            return null;
        }

        return convertToDTO(logs.get(0));
    }

    @Override
    public List<AccessLogDTO> getUserAccessHistory(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "accessTime"));
        Page<AccessLog> logsPage = accessLogRepository.findByUserIdOrderByAccessTimeDesc(userId, pageable);

        return logsPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert AccessLog entity to AccessLogDTO
     */
    private AccessLogDTO convertToDTO(AccessLog log) {
        AccessLogDTO dto = new AccessLogDTO();
        dto.setId(log.getId());
        dto.setUserId(log.getUser().getId());
        dto.setUserName(log.getUser().getFirstName() + " " + log.getUser().getLastName());

        if (log.getZone() != null) {
            dto.setZoneId(log.getZone().getId());
            dto.setZoneName(log.getZone().getName());
        }

        dto.setAccessTime(log.getAccessTime());
        dto.setAccessGranted(log.getAccessGranted());
        dto.setAccessType(log.getAccessType());
        dto.setScannerId(log.getScannerId());
        dto.setScannerLocation(log.getScannerLocation());
        dto.setReasonDenied(log.getReasonDenied());
        dto.setPreviousStatus(log.getPreviousStatus());
        dto.setUpdatedStatus(log.getUpdatedStatus());

        return dto;
    }
}
