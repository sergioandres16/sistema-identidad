package org.saeta.digitalidentitysystem.service;

import org.saeta.digitalidentitysystem.dto.UserDTO;
import org.saeta.digitalidentitysystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User saveUser(User user);
    User updateUser(Long id, User userDetails);
    void deleteUser(Long id);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findAllUsers();
    Page<User> findAllUsers(Pageable pageable);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    void changeUserStatus(Long userId, Long statusId);
    void assignRoleToUser(Long userId, String roleName);
    void removeRoleFromUser(Long userId, String roleName);
    UserDTO getUserDetails(Long userId);
}
