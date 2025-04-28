package org.saeta.digitalidentitysystem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private byte[] profilePhoto;
    private String statusName;
    private String statusColor;
    private Set<String> roles;
    private String studentCode;
    private String faculty;
    private String membershipType;
    private LocalDateTime membershipExpiry;
    private Boolean hasDebt;
}
