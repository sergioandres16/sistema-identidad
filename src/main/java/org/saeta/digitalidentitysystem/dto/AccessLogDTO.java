package org.saeta.digitalidentitysystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccessLogDTO {
    private Long id;
    private Long userId;
    private String userName;
    private Long zoneId;
    private String zoneName;
    private LocalDateTime accessTime;
    private Boolean accessGranted;
    private String accessType;
    private String scannerId;
    private String scannerLocation;
    private String reasonDenied;
    private String previousStatus;
    private String updatedStatus;
}
