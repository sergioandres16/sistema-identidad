package org.saeta.digitalidentitysystem.service;

import org.saeta.digitalidentitysystem.dto.AccessLogDTO;
import org.saeta.digitalidentitysystem.entity.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AccessLogService {
    AccessLog logAccess(Long userId, Long zoneId, boolean accessGranted, String accessType,
                        String scannerId, String scannerLocation, String reasonDenied);

    AccessLog processQrScan(String qrToken, Long zoneId, String scannerId, String scannerLocation);

    AccessLog changeUserStatusOnAccess(Long userId, Long zoneId, Long newStatusId);

    List<AccessLog> findByUserId(Long userId);

    List<AccessLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    Page<AccessLog> findByUserId(Long userId, Pageable pageable);

    AccessLogDTO getLatestAccessForUser(Long userId);

    List<AccessLogDTO> getUserAccessHistory(Long userId, int limit);
}
