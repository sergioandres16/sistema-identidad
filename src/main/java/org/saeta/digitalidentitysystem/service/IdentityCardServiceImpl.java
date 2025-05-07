package org.saeta.digitalidentitysystem.service;

import org.saeta.digitalidentitysystem.dto.CardDTO;
import org.saeta.digitalidentitysystem.entity.IdentityCard;
import org.saeta.digitalidentitysystem.entity.User;
import org.saeta.digitalidentitysystem.entity.UserStatus;
import org.saeta.digitalidentitysystem.exception.ResourceNotFoundException;
import org.saeta.digitalidentitysystem.repository.IdentityCardRepository;
import org.saeta.digitalidentitysystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdentityCardServiceImpl implements IdentityCardService {

    private final IdentityCardRepository identityCardRepository;
    private final UserRepository userRepository;
    private final QrGeneratorService qrGeneratorService;

    @Autowired
    public IdentityCardServiceImpl(
            IdentityCardRepository identityCardRepository,
            UserRepository userRepository,
            QrGeneratorService qrGeneratorService) {
        this.identityCardRepository = identityCardRepository;
        this.userRepository = userRepository;
        this.qrGeneratorService = qrGeneratorService;
    }

    @Override
    @Transactional
    public IdentityCard createCard(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if user already has a card
        Optional<IdentityCard> existingCard = identityCardRepository.findByUserId(userId);
        if (existingCard.isPresent()) {
            return existingCard.get();
        }

        // Create new identity card
        IdentityCard card = new IdentityCard();
        card.setUser(user);
        card.setCardNumber(generateUniqueCardNumber());
        card.setIssueDate(LocalDateTime.now());
        // Configurar expiración a 8 horas en lugar de 1 año
        card.setExpiryDate(LocalDateTime.now().plusHours(8));
        card.setQrSecret(UUID.randomUUID().toString());

        // CAMBIO IMPORTANTE: Iniciar con el carnet deshabilitado
        card.setIsActive(false);

        return identityCardRepository.save(card);
    }

    @Override
    @Transactional
    public IdentityCard updateCard(Long cardId, IdentityCard cardDetails) {
        IdentityCard card = identityCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        // Update card details
        if (cardDetails.getExpiryDate() != null) {
            card.setExpiryDate(cardDetails.getExpiryDate());
        }

        if (cardDetails.getIsActive() != null) {
            card.setIsActive(cardDetails.getIsActive());
        }

        return identityCardRepository.save(card);
    }

    @Override
    @Transactional
    public void deactivateCard(Long cardId) {
        IdentityCard card = identityCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        card.setIsActive(false);
        identityCardRepository.save(card);
    }

    @Override
    @Transactional
    public void activateCard(Long cardId) {
        IdentityCard card = identityCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        card.setIsActive(true);
        identityCardRepository.save(card);
    }

    @Override
    public Optional<IdentityCard> findById(Long id) {
        return identityCardRepository.findById(id);
    }

    @Override
    public Optional<IdentityCard> findByUserId(Long userId) {
        return identityCardRepository.findByUserId(userId);
    }

    @Override
    public Optional<IdentityCard> findByCardNumber(String cardNumber) {
        return identityCardRepository.findByCardNumber(cardNumber);
    }

    @Override
    @Transactional
    public String renewQrCode(Long cardId) {
        IdentityCard card = identityCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        if (!card.getIsActive()) {
            throw new IllegalStateException("Cannot renew QR code for inactive card");
        }

        // Generate new QR code
        return qrGeneratorService.generateQrCodeAsBase64(card.getUser().getId());
    }

    @Override
    public boolean validateCard(Long cardId) {
        Optional<IdentityCard> cardOpt = identityCardRepository.findById(cardId);

        if (cardOpt.isEmpty()) {
            return false;
        }

        IdentityCard card = cardOpt.get();
        User user = card.getUser();

        // Check if card is active
        if (!card.getIsActive()) {
            return false;
        }

        // Check if card is expired
        if (card.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Check user status
        if (user.getStatus() != null &&
                (user.getStatus().getName().equals(UserStatus.INACTIVE) ||
                        user.getStatus().getName().equals(UserStatus.SUSPENDED))) {
            return false;
        }

        // Check debt status for club members
        if (user.getHasDebt() != null && user.getHasDebt() &&
                user.getMembershipType() != null && !user.getMembershipType().isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public CardDTO getCardDetails(Long cardId) {
        IdentityCard card = identityCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        return convertToDTO(card);
    }

    @Override
    public CardDTO getCardDetailsForUser(Long userId) {
        IdentityCard card = identityCardRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found for user with id: " + userId));

        return convertToDTO(card);
    }

    /**
     * Convert IdentityCard entity to CardDTO
     */
    private CardDTO convertToDTO(IdentityCard card) {
        CardDTO cardDTO = new CardDTO();
        cardDTO.setId(card.getId());
        cardDTO.setCardNumber(card.getCardNumber());
        cardDTO.setIssueDate(card.getIssueDate());
        cardDTO.setExpiryDate(card.getExpiryDate());
        cardDTO.setIsActive(card.getIsActive());

        // 1️⃣  Imagen PNG del QR en Base-64 (la que el front mostrará)
        String qrCode = qrGeneratorService.generateQrCodeAsBase64(card.getUser().getId());
        cardDTO.setQrCodeBase64(qrCode);

        // 2️⃣  👉 NUEVA línea: token “crudo” que va dentro del QR
        cardDTO.setQrToken(card.getLastQrCode());

        // ---------- datos del usuario ----------
        User user = card.getUser();
        cardDTO.setUserId(user.getId());
        cardDTO.setFirstName(user.getFirstName());
        cardDTO.setLastName(user.getLastName());
        cardDTO.setProfilePhoto(user.getProfilePhoto());

        if (user.getStatus() != null) {
            cardDTO.setStatus(user.getStatus().getName());
            cardDTO.setStatusColor(user.getStatus().getStatusColor());
        }

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            cardDTO.setRole(user.getRoles().iterator().next().getName());
        }

        return cardDTO;
    }

    /**
     * Generate a unique card number
     */
    private String generateUniqueCardNumber() {
        String cardNumber;
        boolean isUnique = false;

        do {
            // Generate a random card number with format SAETA-XXXXXX
            cardNumber = "SAETA-" + String.format("%06d", (int)(Math.random() * 1000000));

            // Check if it's unique
            Optional<IdentityCard> existingCard = identityCardRepository.findByCardNumber(cardNumber);
            isUnique = existingCard.isEmpty();
        } while (!isUnique);

        return cardNumber;
    }
}