package com.buildmaster.projecttracker.security.oauth2;

import com.buildmaster.projecttracker.entity.Role;
import com.buildmaster.projecttracker.entity.User;
import com.buildmaster.projecttracker.enums.AuthProvider;
import com.buildmaster.projecttracker.repository.RoleRepository;
import com.buildmaster.projecttracker.repository.UserRepository;
import com.buildmaster.projecttracker.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (OAuth2AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing OAuth2 user", ex);
            OAuth2Error error = new OAuth2Error("oauth2_processing_error",
                    "An unexpected error occurred during OAuth2 authentication", null);
            throw new OAuth2AuthenticationException(error, ex.getMessage(), ex);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        log.info("Processing OAuth2 user for provider: {}", registrationId);
        log.debug("OAuth2 user attributes: {}", attributes);
        log.debug("OAuth2 user authorities: {}", oAuth2User.getAuthorities());

        String email = extractEmail(userRequest, attributes, registrationId);

        log.info("Extracted email: {} for provider: {}", email, registrationId);

        if (email == null || email.trim().isEmpty()) {
            log.error("No email found for OAuth2 provider: {}. Available attributes: {}",
                    registrationId, attributes.keySet());
            OAuth2Error error = new OAuth2Error("email_not_found",
                    "No email address found from OAuth2 provider: " + registrationId, null);
            throw new OAuth2AuthenticationException(error, error.getDescription());
        }

        attributes.put("email", email);

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            log.info("Found existing user: {} with auth provider: {}", email, user.getAuthProvider());

            if (!isMatchingAuthProvider(user.getAuthProvider(), registrationId)) {
                String errorMsg = String.format("Account exists with %s provider. Please use %s to login.",
                        user.getAuthProvider(), user.getAuthProvider());
                log.warn("Auth provider mismatch for user {}: expected {}, got {}",
                        email, user.getAuthProvider(), registrationId);
                throw new OAuth2AuthenticationException(errorMsg);
            }
            user = updateExistingUser(user, attributes);
        } else {
            log.info("Registering new user with email: {}", email);
            user = registerNewUser(userRequest, attributes);
        }

        log.info("OAuth2 authentication successful for user: {}", user.getEmail());
        return new CustomOAuth2User(user, attributes);
    }

    private String extractEmail(OAuth2UserRequest userRequest, Map<String, Object> attributes, String registrationId) {
        String email = null;

        if ("google".equalsIgnoreCase(registrationId)) {
            email = extractGoogleEmail(attributes);
        } else if ("github".equalsIgnoreCase(registrationId)) {
            email = extractGitHubEmail(userRequest, attributes);
        } else {
            email = extractGenericEmail(attributes);
        }

        return email;
    }

    private String extractGoogleEmail(Map<String, Object> attributes) {
        Object emailObj = attributes.get("email");
        if (emailObj instanceof String && !((String) emailObj).trim().isEmpty()) {
            String email = ((String) emailObj).trim();
            log.debug("Found Google email: {}", email);
            return email;
        }
        log.warn("Google OAuth2 user has no valid email attribute");
        return null;
    }

    private String extractGitHubEmail(OAuth2UserRequest userRequest, Map<String, Object> attributes) {
        Object emailObj = attributes.get("email");
        if (emailObj instanceof String && !((String) emailObj).trim().isEmpty()) {
            String email = ((String) emailObj).trim();
            log.debug("Found GitHub email in attributes: {}", email);
            return email;
        }

        log.info("No email in GitHub attributes, fetching from GitHub API");
        String token = userRequest.getAccessToken().getTokenValue();

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            headers.add("Accept", "application/vnd.github.v3+json");
            headers.add("User-Agent", "ProjectTracker-OAuth");

            HttpEntity<String> entity = new HttpEntity<>("", headers);
            ResponseEntity<List> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            List<Map<String, Object>> emails = response.getBody();
            if (emails != null && !emails.isEmpty()) {
                log.debug("Retrieved {} emails from GitHub API", emails.size());

                for (Map<String, Object> mail : emails) {
                    Boolean primary = (Boolean) mail.get("primary");
                    Boolean verified = (Boolean) mail.get("verified");
                    String emailAddr = (String) mail.get("email");

                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified) &&
                            emailAddr != null && !emailAddr.trim().isEmpty()) {
                        log.debug("Found primary verified GitHub email: {}", emailAddr);
                        return emailAddr.trim();
                    }
                }

                for (Map<String, Object> mail : emails) {
                    Boolean verified = (Boolean) mail.get("verified");
                    String emailAddr = (String) mail.get("email");

                    if (Boolean.TRUE.equals(verified) && emailAddr != null && !emailAddr.trim().isEmpty()) {
                        log.debug("Found verified GitHub email: {}", emailAddr);
                        return emailAddr.trim();
                    }
                }

                for (Map<String, Object> mail : emails) {
                    String emailAddr = (String) mail.get("email");
                    if (emailAddr != null && !emailAddr.trim().isEmpty()) {
                        log.warn("Using unverified GitHub email: {}", emailAddr);
                        return emailAddr.trim();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch GitHub user emails: {}", e.getMessage(), e);
        }

        log.warn("No valid email found for GitHub user");
        return null;
    }

    private String extractGenericEmail(Map<String, Object> attributes) {
        Object emailObj = attributes.get("email");
        if (emailObj instanceof String && !((String) emailObj).trim().isEmpty()) {
            String email = ((String) emailObj).trim();
            log.debug("Found generic provider email: {}", email);
            return email;
        }
        return null;
    }

    private boolean isMatchingAuthProvider(AuthProvider userAuthProvider, String registrationId) {
        if (userAuthProvider == null || registrationId == null) {
            return false;
        }

        String userProviderName = userAuthProvider.name().toLowerCase();
        String registrationIdLower = registrationId.toLowerCase();

        return userProviderName.equals(registrationIdLower) ||
                userProviderName.startsWith(registrationIdLower) ||
                registrationIdLower.startsWith(userProviderName);
    }

    private User registerNewUser(OAuth2UserRequest userRequest, Map<String, Object> attributes) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        UserInfo userInfo = extractUserInfo(registrationId, attributes);

        try {
            AuthProvider authProvider = AuthProvider.valueOf(registrationId.toUpperCase());

            User user = User.builder()
                    .username(generateUniqueUsername(userInfo.name, userInfo.email))
                    .email(userInfo.email)
                    .firstName(userInfo.firstName)
                    .lastName(userInfo.lastName)
                    .authProvider(authProvider)
                    .providerId(userInfo.providerId)
                    .emailVerified(true)
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .build();

            Role defaultRole = roleRepository.findByName("ROLE_CONTRACTOR")
                    .orElseThrow(() -> new RuntimeException("Default OAuth2 role ROLE_CONTRACTOR not found"));

            Set<Role> roles = new HashSet<>();
            roles.add(defaultRole);
            user.setRoles(roles);

            User savedUser = userRepository.save(user);

            auditService.logUserRegistration(savedUser);

            log.info("New OAuth2 user registered: {} via {}", savedUser.getEmail(), savedUser.getAuthProvider());

            return savedUser;
        } catch (IllegalArgumentException e) {
            log.error("Invalid auth provider: {}", registrationId);
            throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
        }
    }

    private User updateExistingUser(User existingUser, Map<String, Object> attributes) {
        String registrationId = existingUser.getAuthProvider().name().toLowerCase();
        UserInfo userInfo = extractUserInfo(registrationId, attributes);

        existingUser.setFirstName(userInfo.firstName);
        existingUser.setLastName(userInfo.lastName);

        User updatedUser = userRepository.save(existingUser);

        log.info("Existing OAuth2 user updated: {}", updatedUser.getEmail());

        return updatedUser;
    }

    private UserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        String providerId;
        String firstName = null;
        String lastName = null;
        String name = (String) attributes.get("name");
        String email = (String) attributes.get("email");

        if ("google".equalsIgnoreCase(registrationId)) {
            providerId = (String) attributes.get("sub");
            firstName = (String) attributes.get("given_name");
            lastName = (String) attributes.get("family_name");
        } else if ("github".equalsIgnoreCase(registrationId)) {
            providerId = String.valueOf(attributes.get("id"));
            if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split("\\s+", 2);
                firstName = parts[0];
                if (parts.length > 1) {
                    lastName = parts[1];
                }
            }
        } else {
            Object idObj = attributes.get("id");
            providerId = idObj != null ? String.valueOf(idObj) : null;
            firstName = (String) attributes.get("given_name");
            lastName = (String) attributes.get("family_name");
            if ((firstName == null || lastName == null) && name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split("\\s+", 2);
                if (firstName == null) firstName = parts[0];
                if (lastName == null && parts.length > 1) lastName = parts[1];
            }
        }

        return new UserInfo(providerId, firstName, lastName, name, email);
    }

    private String generateUniqueUsername(String name, String email) {
        String baseUsername;

        if (name != null && !name.trim().isEmpty()) {
            baseUsername = name.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
        } else if (email != null && !email.trim().isEmpty()) {
            baseUsername = email.split("@")[0].toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
        } else {
            baseUsername = "user";
        }
        if (baseUsername.length() < 3) {
            baseUsername = "user" + baseUsername;
        }

        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
    private static class UserInfo {
        final String providerId;
        final String firstName;
        final String lastName;
        final String name;
        final String email;

        UserInfo(String providerId, String firstName, String lastName, String name, String email) {
            this.providerId = providerId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.name = name;
            this.email = email;
        }
    }
}