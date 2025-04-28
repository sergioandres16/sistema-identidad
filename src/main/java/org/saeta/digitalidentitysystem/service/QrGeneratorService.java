package org.saeta.digitalidentitysystem.service;

import java.awt.image.BufferedImage;

/**
 * Service interface for generating QR codes
 */
public interface QrGeneratorService {

    /**
     * Generate a QR code for a user
     *
     * @param userId the ID of the user
     * @return a BufferedImage containing the QR code
     */
    BufferedImage generateQrCodeForUser(Long userId);

    /**
     * Generate a token for QR code
     *
     * @param userId the ID of the user
     * @return token string
     */
    String generateTokenForUser(Long userId);

    /**
     * Generate QR code as base64 string
     *
     * @param userId the ID of the user
     * @return base64 encoded QR code image
     */
    String generateQrCodeAsBase64(Long userId);

    /**
     * Validate a QR token
     *
     * @param token the token to validate
     * @return the user ID if valid, otherwise null
     */
    Long validateQrToken(String token);
}
