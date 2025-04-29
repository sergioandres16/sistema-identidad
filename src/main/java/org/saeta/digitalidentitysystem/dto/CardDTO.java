package org.saeta.digitalidentitysystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CardDTO {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private byte[] profilePhoto;
    private String cardNumber;
    private String qrToken;
    private LocalDateTime issueDate;
    private LocalDateTime expiryDate;
    private Boolean isActive;
    private String status;
    private String statusColor;
    private String role;
    private String qrCodeBase64;
}