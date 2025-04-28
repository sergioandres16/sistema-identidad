package org.saeta.digitalidentitysystem.service;

import org.saeta.digitalidentitysystem.dto.CardDTO;
import org.saeta.digitalidentitysystem.entity.IdentityCard;

import java.util.Optional;

public interface IdentityCardService {
    IdentityCard createCard(Long userId);
    IdentityCard updateCard(Long cardId, IdentityCard cardDetails);
    void deactivateCard(Long cardId);
    void activateCard(Long cardId);
    Optional<IdentityCard> findById(Long id);
    Optional<IdentityCard> findByUserId(Long userId);
    Optional<IdentityCard> findByCardNumber(String cardNumber);
    String renewQrCode(Long cardId);
    boolean validateCard(Long cardId);
    CardDTO getCardDetails(Long cardId);
    CardDTO getCardDetailsForUser(Long userId);
}
