package org.saeta.digitalidentitysystem.dto;

import lombok.Data;

@Data
public class QrValidationResponse {
    private boolean valid;
    private Long userId;
    private String userName;
    private String userStatus;
    private String statusColor;
    private String userRole;
    private byte[] profilePhoto;
    private boolean accessGranted;
    private String reasonDenied;
    private Long logId;
}
