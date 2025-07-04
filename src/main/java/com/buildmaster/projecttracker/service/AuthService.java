package com.buildmaster.projecttracker.service;

import com.buildmaster.projecttracker.dto.AuthResponse;
import com.buildmaster.projecttracker.dto.LoginRequest;
import com.buildmaster.projecttracker.dto.RegisterRequest;
import com.buildmaster.projecttracker.enums.AuthProvider;
import com.buildmaster.projecttracker.entity.Role;
import com.buildmaster.projecttracker.entity.User;
import com.buildmaster.projecttracker.repository.RoleRepository;
import com.buildmaster.projecttracker.repository.UserRepository;
import com.buildmaster.projecttracker.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(passwordEncoder.encode(request.getPassword()))
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(false)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        Set<Role> userRoles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String roleName : request.getRoles()) {
                Role role = roleRepository.findByName("ROLE_" + roleName.toUpperCase())
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
                userRoles.add(role);
            }
        } else {
            Role defaultRole = roleRepository.findByName("ROLE_DEVELOPER")
                    .orElseThrow(() -> new RuntimeException("Default role ROLE_DEVELOPER not found"));
            userRoles.add(defaultRole);
        }

        user.setRoles(userRoles);
        User savedUser = userRepository.save(user);

        String accessToken = jwtUtil.generateToken(savedUser.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getUsername());

        auditService.logUserRegistration(savedUser);
        return buildAuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            String accessToken = jwtUtil.generateToken(authentication);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
            auditService.logUserLogin(user, true);

            return buildAuthResponse(accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                    .ifPresent(user -> auditService.logUserLogin(user, false));

            throw new RuntimeException("Invalid username/email or password");
        }
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtUtil.generateToken(username);
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        return buildAuthResponse(newAccessToken, newRefreshToken);
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken) {

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(1800L)
                .build();
    }
}