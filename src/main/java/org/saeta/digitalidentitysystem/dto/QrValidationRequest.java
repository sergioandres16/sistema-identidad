package org.saeta.digitalidentitysystem.dto;

import lombok.Data;

@Data
public class QrValidationRequest {
    private String qrToken;
    private Long zoneId;
    private String scannerId;
    private String scannerLocation;
}
