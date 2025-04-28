package org.saeta.digitalidentitysystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "identity_cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentityCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "card_number", unique = true)
    private String cardNumber;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "qr_secret", nullable = false)
    private String qrSecret;

    @Column(name = "last_qr_timestamp")
    private LocalDateTime lastQrTimestamp;

    @Column(name = "last_qr_code")
    private String lastQrCode;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
