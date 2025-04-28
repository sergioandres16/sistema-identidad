package org.saeta.digitalidentitysystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_statuses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @Column(name = "status_color")
    private String statusColor; // Hex color code

    // Common status names
    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";
    public static final String SUSPENDED = "SUSPENDED";
    public static final String PENDING = "PENDING";
    public static final String EXPIRED = "EXPIRED";
    public static final String DEBT = "DEBT";
}
