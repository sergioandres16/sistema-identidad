package org.saeta.digitalidentitysystem.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.saeta.digitalidentitysystem.entity.IdentityCard;
import org.saeta.digitalidentitysystem.entity.User;
import org.saeta.digitalidentitysystem.exception.ResourceNotFoundException;
import org.saeta.digitalidentitysystem.repository.IdentityCardRepository;
import org.saeta.digitalidentitysystem.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
public class QrGeneratorServiceImpl implements QrGeneratorService {

    @Value("${qr.width}")
    private int qrWidth;

    @Value("${qr.height}")
    private int qrHeight;

    @Value("${qr.expiration.seconds:30}") // Default QR expiration (8 hours)
    private int qrExpirationSeconds;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${frontend.base.url:http://192.168.18.45:4200}")
    private String frontendBaseUrl;

    @Value("${qr.redirect.url:http://192.168.18.45:4200/qr-redirect.html?token=}")
    private String qrRedirectUrl;

    // NUEVO: duración de la habilitación del carnet (8 horas por defecto)
    @Value("${card.activation.duration.hours:8}")
    private int cardActivationDurationHours;

    private final UserRepository userRepository;
    private final IdentityCardRepository identityCardRepository;

    @Autowired
    public QrGeneratorServiceImpl(UserRepository userRepository, IdentityCardRepository identityCardRepository) {
        this.userRepository = userRepository;
        this.identityCardRepository = identityCardRepository;
    }

    @Override
    public BufferedImage generateQrCodeForUser(Long userId) {
        String redirectUrl = generateTokenForUser(userId);
        return generateQrCodeFromString(redirectUrl);
    }

    @Override
    public String generateTokenForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        IdentityCard card = identityCardRepository.findByUserId(userId)
                .orElseGet(() -> {
                    IdentityCard newCard = new IdentityCard();
                    newCard.setUser(user);
                    newCard.setCardNumber(UUID.randomUUID().toString());
                    newCard.setIssueDate(LocalDateTime.now());
                    // Cambiado: expiración a 8 horas
                    newCard.setExpiryDate(LocalDateTime.now().plusHours(cardActivationDurationHours));  // Activación del carnet
                    newCard.setQrSecret(UUID.randomUUID().toString());
                    newCard.setIsActive(false);  // Comienza desactivado
                    return identityCardRepository.save(newCard);
                });

        Date issuedAt = new Date();
        Date expiryDate = new Date(issuedAt.getTime() + qrExpirationSeconds * 1000); // Expiración del QR token

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

        String token = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(issuedAt)
                .setExpiration(expiryDate)
                .claim("cardId", card.getId())
                .claim("nonce", UUID.randomUUID().toString())
                .signWith(key)
                .compact();

        String redirectUrl = qrRedirectUrl + token;

        card.setLastQrTimestamp(LocalDateTime.now());
        card.setLastQrCode(token);
        identityCardRepository.save(card);

        return redirectUrl;
    }

    @Override
    public String generateQrCodeAsBase64(Long userId) {
        BufferedImage qrImage = generateQrCodeForUser(userId);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeBase64String(imageBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    @Override
    public Long validateQrToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userIdStr = claims.getSubject();
            Long userId = Long.parseLong(userIdStr);

            Date expirationDate = claims.getExpiration();
            if (expirationDate.before(new Date())) {
                return null;
            }

            Long cardId = claims.get("cardId", Long.class);
            IdentityCard card = identityCardRepository.findById(cardId)
                    .orElse(null);

            if (card == null || !card.getIsActive()) {
                return null;
            }

            return userId;
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage generateQrCodeFromString(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, qrWidth, qrHeight);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (WriterException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
}