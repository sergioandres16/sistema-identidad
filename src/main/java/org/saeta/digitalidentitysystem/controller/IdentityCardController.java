package org.saeta.digitalidentitysystem.controller;

import org.saeta.digitalidentitysystem.dto.CardDTO;
import org.saeta.digitalidentitysystem.entity.IdentityCard;
import org.saeta.digitalidentitysystem.service.IdentityCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cards")
public class IdentityCardController {

    private final IdentityCardService identityCardService;

    @Autowired
    public IdentityCardController(IdentityCardService identityCardService) {
        this.identityCardService = identityCardService;
    }

    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IdentityCard> createCard(@PathVariable Long userId) {
        IdentityCard card = identityCardService.createCard(userId);
        return new ResponseEntity<>(card, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<CardDTO> getCardById(@PathVariable Long id) {
        CardDTO cardDTO = identityCardService.getCardDetails(id);
        return new ResponseEntity<>(cardDTO, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public ResponseEntity<CardDTO> getCardByUserId(@PathVariable Long userId) {
        CardDTO cardDTO = identityCardService.getCardDetailsForUser(userId);
        return new ResponseEntity<>(cardDTO, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IdentityCard> updateCard(@PathVariable Long id, @RequestBody IdentityCard cardDetails) {
        IdentityCard updatedCard = identityCardService.updateCard(id, cardDetails);
        return new ResponseEntity<>(updatedCard, HttpStatus.OK);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateCard(@PathVariable Long id) {
        identityCardService.deactivateCard(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateCard(@PathVariable Long id) {
        identityCardService.activateCard(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/{id}/renew-qr")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<String> renewQrCode(@PathVariable Long id) {
        String qrBase64 = identityCardService.renewQrCode(id);
        return new ResponseEntity<>(qrBase64, HttpStatus.OK);
    }

    @GetMapping("/{id}/validate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SCANNER')")
    public ResponseEntity<Boolean> validateCard(@PathVariable Long id) {
        boolean isValid = identityCardService.validateCard(id);
        return new ResponseEntity<>(isValid, HttpStatus.OK);
    }
}
