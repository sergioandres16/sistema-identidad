package org.saeta.digitalidentitysystem.repository;

import org.saeta.digitalidentitysystem.entity.AccessZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccessZoneRepository extends JpaRepository<AccessZone, Long> {
    Optional<AccessZone> findByName(String name);
}
