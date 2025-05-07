package org.saeta.digitalidentitysystem.controller;

import org.saeta.digitalidentitysystem.dto.LoginRequest;
import org.saeta.digitalidentitysystem.dto.LoginResponse;
import org.saeta.digitalidentitysystem.dto.RegisterRequest;
import org.saeta.digitalidentitysystem.entity.Role;
import org.saeta.digitalidentitysystem.entity.User;
import org.saeta.digitalidentitysystem.entity.UserStatus;
import org.saeta.digitalidentitysystem.repository.RoleRepository;
import org.saeta.digitalidentitysystem.repository.UserStatusRepository;
import org.saeta.digitalidentitysystem.security.JwtTokenProvider;
import org.saeta.digitalidentitysystem.service.IdentityCardService;
import org.saeta.digitalidentitysystem.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final UserStatusRepository userStatusRepository;
    private final IdentityCardService identityCardService;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthController(
            AuthenticationManager authenticationManager,
            UserService userService,
            RoleRepository roleRepository,
            UserStatusRepository userStatusRepository,
            IdentityCardService identityCardService,
            JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.userStatusRepository = userStatusRepository;
        this.identityCardService = identityCardService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);

        User user = userService.findByUsername(loginRequest.getUsername()).orElse(null);

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(jwt);

        if (user != null) {
            loginResponse.setId(user.getId());
            loginResponse.setUsername(user.getUsername());
            loginResponse.setFirstName(user.getFirstName());
            loginResponse.setLastName(user.getLastName());
            loginResponse.setEmail(user.getEmail());

            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                loginResponse.setRole(user.getRoles().iterator().next().getName());
            }
        }

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userService.existsByUsername(registerRequest.getUsername())) {
            return new ResponseEntity<>("Username is already taken!", HttpStatus.BAD_REQUEST);
        }

        if (userService.existsByEmail(registerRequest.getEmail())) {
            return new ResponseEntity<>("Email is already in use!", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword()); // Will be encoded in service
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEmail(registerRequest.getEmail());
        user.setPhoneNumber(registerRequest.getPhoneNumber());

        // Set student information if provided
        if (registerRequest.getStudentCode() != null && !registerRequest.getStudentCode().isEmpty()) {
            user.setStudentCode(registerRequest.getStudentCode());
            user.setFaculty(registerRequest.getFaculty());
        }
        // Set membership information if provided
        else if (registerRequest.getMembershipType() != null && !registerRequest.getMembershipType().isEmpty()) {
            user.setMembershipType(registerRequest.getMembershipType());
            // Set membership expiry to 1 year from now
            user.setMembershipExpiry(LocalDateTime.now().plusYears(1));
            user.setHasDebt(false);
        }

        // Set user role
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);
        user.setRoles(roles);

        // CAMBIO IMPORTANTE: Set user status to INACTIVE by default instead of ACTIVE
        UserStatus inactiveStatus = userStatusRepository.findByName(UserStatus.INACTIVE)
                .orElseThrow(() -> new RuntimeException("Error: Status is not found."));
        user.setStatus(inactiveStatus);

        User savedUser = userService.saveUser(user);

        // Create identity card for the user
        identityCardService.createCard(savedUser.getId());

        return new ResponseEntity<>("User registered successfully!", HttpStatus.CREATED);
    }
}