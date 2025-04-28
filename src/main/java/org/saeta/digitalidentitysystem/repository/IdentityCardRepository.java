package org.saeta.digitalidentitysystem.repository;

import org.saeta.digitalidentitysystem.entity.IdentityCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentityCardRepository extends JpaRepository<IdentityCard, Long> {
    Optional<IdentityCard> findByUserId(Long userId);
    Optional<IdentityCard> findByCardNumber(String cardNumber);
    Optional<IdentityCard> findByLastQrCode(String qrCode);
}
