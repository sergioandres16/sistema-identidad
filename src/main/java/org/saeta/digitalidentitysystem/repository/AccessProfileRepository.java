package org.saeta.digitalidentitysystem.repository;

import org.saeta.digitalidentitysystem.entity.AccessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccessProfileRepository extends JpaRepository<AccessProfile, Long> {
    Optional<AccessProfile> findByName(String name);
}
