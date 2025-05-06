package org.saeta.digitalidentitysystem.controller;

import org.saeta.digitalidentitysystem.dto.AccessLogDTO;
import org.saeta.digitalidentitysystem.dto.QrValidationRequest;
import org.saeta.digitalidentitysystem.dto.QrValidationResponse;
import org.saeta.digitalidentitysystem.entity.AccessLog;
import org.saeta.digitalidentitysystem.entity.User;
import org.saeta.digitalidentitysystem.service.AccessLogService;
import org.saeta.digitalidentitysystem.service.QrGeneratorService;
import org.saeta.digitalidentitysystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/access-logs")
public class AccessLogController {

    private final AccessLogService accessLogService;
    private final QrGeneratorService qrGeneratorService;
    private final UserService userService;

    @Autowired
    public AccessLogController(
            AccessLogService accessLogService,
            QrGeneratorService qrGeneratorService,
            UserService userService) {
        this.accessLogService = accessLogService;
        this.qrGeneratorService = qrGeneratorService;
        this.userService = userService;
    }

    @PostMapping("/validate-qr")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SCANNER')")
    public ResponseEntity<QrValidationResponse> validateQrCode(@RequestBody QrValidationRequest request) {
        Long userId = qrGeneratorService.validateQrToken(request.getQrToken());

        QrValidationResponse response = new QrValidationResponse();
        if (userId == null) {
            response.setValid(false);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        AccessLog accessLog = accessLogService.processQrScan(
                request.getQrToken(),
                request.getZoneId(),
                request.getScannerId(),
                request.getScannerLocation()
        );

        User user = userService.findById(userId).orElse(null);
        if (user != null) {
            response.setValid(true);
            response.setUserId(userId);
            response.setUserName(user.getFirstName() + " " + user.getLastName());

            if (user.getStatus() != null) {
                response.setUserStatus(user.getStatus().getName());
                response.setStatusColor(user.getStatus().getStatusColor());
            }

            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                response.setUserRole(user.getRoles().iterator().next().getName());
            }

            response.setProfilePhoto(user.getProfilePhoto());
            response.setAccessGranted(accessLog.getAccessGranted());
            response.setReasonDenied(accessLog.getReasonDenied());
            response.setLogId(accessLog.getId());
        } else {
            response.setValid(false);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/change-status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SCANNER')")
    public ResponseEntity<AccessLog> changeUserStatusOnAccess(
            @RequestParam Long userId,
            @RequestParam Long zoneId,
            @RequestParam Long newStatusId) {

        AccessLog accessLog = accessLogService.changeUserStatusOnAccess(userId, zoneId, newStatusId);
        return new ResponseEntity<>(accessLog, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public ResponseEntity<List<AccessLogDTO>> getUserAccessHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {

        List<AccessLogDTO> accessLogs = accessLogService.getUserAccessHistory(userId, limit);
        return new ResponseEntity<>(accessLogs, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}/latest")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public ResponseEntity<AccessLogDTO> getLatestUserAccess(@PathVariable Long userId) {
        AccessLogDTO latestAccess = accessLogService.getLatestAccessForUser(userId);

        if (latestAccess == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(latestAccess, HttpStatus.OK);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccessLog>> getAccessLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<AccessLog> accessLogs = accessLogService.findByDateRange(startDate, endDate);
        return new ResponseEntity<>(accessLogs, HttpStatus.OK);
    }
}
