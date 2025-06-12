package com.buildmaster.projecttracker.controller;

import com.buildmaster.projecttracker.dto.AuthResponse;
import com.buildmaster.projecttracker.dto.LoginRequest;
import com.buildmaster.projecttracker.dto.RegisterRequest;
import com.buildmaster.projecttracker.entity.User;
import com.buildmaster.projecttracker.service.AuthService;
import com.buildmaster.projecttracker.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.login(request);

            // Set JWT as HttpOnly cookie using CookieUtils
            CookieUtils.addCookie(response, "jwt", authResponse.getAccessToken(), 24 * 60 * 60);

            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // Use deleteCookie for JWT
        CookieUtils.deleteCookie(request, response, "jwt");

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Logout successful");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/oauth2/success")
    public ResponseEntity<AuthResponse> oauth2LoginSuccess(@RequestParam String token) {
        // This endpoint is called after successful OAuth2 authentication
        // The token is generated in OAuth2AuthenticationSuccessHandler
        AuthResponse response = AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .build();
        return ResponseEntity.ok(response);
    }
}