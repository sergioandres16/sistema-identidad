package org.saeta.digitalidentitysystem.repository;

import org.saeta.digitalidentitysystem.entity.AccessLog;
import org.saeta.digitalidentitysystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    List<AccessLog> findByUser(User user);
    List<AccessLog> findByUserIdOrderByAccessTimeDesc(Long userId);
    List<AccessLog> findByAccessTimeBetween(LocalDateTime start, LocalDateTime end);
    Page<AccessLog> findByUserIdOrderByAccessTimeDesc(Long userId, Pageable pageable);
}
