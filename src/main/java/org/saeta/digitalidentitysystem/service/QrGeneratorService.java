package org.saeta.digitalidentitysystem.service;

import java.awt.image.BufferedImage;

public interface QrGeneratorService {

    BufferedImage generateQrCodeForUser(Long userId);

    String generateTokenForUser(Long userId);

    String generateQrCodeAsBase64(Long userId);

    Long validateQrToken(String token);
}
