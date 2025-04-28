package org.saeta.digitalidentitysystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "access_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "profile_zones",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "zone_id")
    )
    private Set<AccessZone> allowedZones = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "profile_time_restrictions",
            joinColumns = @JoinColumn(name = "profile_id")
    )
    private Set<TimeRestriction> timeRestrictions = new HashSet<>();

    @Embeddable
    @Data
    public static class TimeRestriction {
        @Enumerated(EnumType.STRING)
        @Column(name = "day_of_week")
        private DayOfWeek dayOfWeek;

        @Column(name = "start_time")
        private LocalTime startTime;

        @Column(name = "end_time")
        private LocalTime endTime;
    }
}
