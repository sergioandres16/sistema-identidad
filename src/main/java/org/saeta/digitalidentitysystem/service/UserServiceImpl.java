package org.saeta.digitalidentitysystem.service;

import org.saeta.digitalidentitysystem.dto.UserDTO;
import org.saeta.digitalidentitysystem.entity.Role;
import org.saeta.digitalidentitysystem.entity.User;
import org.saeta.digitalidentitysystem.entity.UserStatus;
import org.saeta.digitalidentitysystem.exception.ResourceNotFoundException;
import org.saeta.digitalidentitysystem.repository.RoleRepository;
import org.saeta.digitalidentitysystem.repository.UserRepository;
import org.saeta.digitalidentitysystem.repository.UserStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserStatusRepository userStatusRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserStatusRepository userStatusRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userStatusRepository = userStatusRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setEmail(userDetails.getEmail());
        user.setPhoneNumber(userDetails.getPhoneNumber());

        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        if (userDetails.getStudentCode() != null) {
            user.setStudentCode(userDetails.getStudentCode());
        }
        if (userDetails.getFaculty() != null) {
            user.setFaculty(userDetails.getFaculty());
        }

        if (userDetails.getMembershipType() != null) {
            user.setMembershipType(userDetails.getMembershipType());
        }
        if (userDetails.getMembershipExpiry() != null) {
            user.setMembershipExpiry(userDetails.getMembershipExpiry());
        }
        if (userDetails.getHasDebt() != null) {
            user.setHasDebt(userDetails.getHasDebt());
        }

        if (userDetails.getProfilePhoto() != null) {
            user.setProfilePhoto(userDetails.getProfilePhoto());
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void changeUserStatus(Long userId, Long statusId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserStatus status = userStatusRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + statusId));

        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void assignRoleToUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + roleName));

        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void removeRoleFromUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + roleName));

        user.getRoles().remove(role);
        userRepository.save(user);
    }

    @Override
    public UserDTO getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setEmail(user.getEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setProfilePhoto(user.getProfilePhoto());

        if (user.getStatus() != null) {
            userDTO.setStatusName(user.getStatus().getName());
            userDTO.setStatusColor(user.getStatus().getStatusColor());
        }

        if (user.getRoles() != null) {
            userDTO.setRoles(user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet()));
        }

        userDTO.setStudentCode(user.getStudentCode());
        userDTO.setFaculty(user.getFaculty());
        userDTO.setMembershipType(user.getMembershipType());
        userDTO.setMembershipExpiry(user.getMembershipExpiry());
        userDTO.setHasDebt(user.getHasDebt());

        return userDTO;
    }
}
