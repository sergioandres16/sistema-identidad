package org.saeta.digitalidentitysystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "zone_id")
    private AccessZone zone;

    @Column(name = "access_time", nullable = false)
    private LocalDateTime accessTime;

    @Column(name = "access_granted")
    private Boolean accessGranted;

    @Column(name = "access_type")
    private String accessType; // ENTRY, EXIT

    @Column(name = "scanner_id")
    private String scannerId;

    @Column(name = "scanner_location")
    private String scannerLocation;

    @Column(name = "reason_denied")
    private String reasonDenied;

    @Column(name = "previous_status")
    private String previousStatus;

    @Column(name = "updated_status")
    private String updatedStatus;
}
