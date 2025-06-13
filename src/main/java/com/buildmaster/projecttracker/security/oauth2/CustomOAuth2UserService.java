package com.buildmaster.projecttracker.security.oauth2;

import com.buildmaster.projecttracker.entity.AuthProvider;
import com.buildmaster.projecttracker.entity.Role;
import com.buildmaster.projecttracker.entity.User;
import com.buildmaster.projecttracker.repository.RoleRepository;
import com.buildmaster.projecttracker.repository.UserRepository;
import com.buildmaster.projecttracker.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException("Error processing OAuth2 user: " + ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        logger.info("OAuth2 user attributes: {}", attributes);

        if (attributes == null) {
            logger.error("OAuth2 attributes map is null!");
            throw new OAuth2AuthenticationException("OAuth2 attributes map is null");
        }

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Object emailObj = attributes.get("email");

        if (emailObj == null && "github".equals(registrationId)) {
            logger.warn("No email attribute found in OAuth2 user attributes. Provider: {}", registrationId);
            // Fetch email from GitHub /user/emails endpoint
            String email = fetchGithubEmail(userRequest.getAccessToken().getTokenValue());
            if (email != null) {
                attributes = new HashMap<>(attributes); // make mutable
                attributes.put("email", email);
            } else {
                throw new OAuth2AuthenticationException("Email not found in OAuth2 user attributes or GitHub /user/emails endpoint");
            }
        }

        // Example: check for expected attribute (e.g., "email")
        if (!attributes.containsKey("email")) {
            logger.warn("OAuth2 user does not contain 'email' attribute. Attributes: {}", attributes);
            // Optionally, handle fetching email from /user/emails endpoint here
        }

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);

        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(userInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getAuthProvider().equals(AuthProvider.valueOf(registrationId.toUpperCase()))) {
                throw new OAuth2AuthenticationException(
                        "Looks like you're signed up with " + user.getAuthProvider() +
                                " account. Please use your " + user.getAuthProvider() + " account to login."
                );
            }
            user = updateExistingUser(user, userInfo);
        } else {
            user = registerNewUser(userRequest, userInfo);
        }

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest userRequest, OAuth2UserInfo userInfo) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        User user = User.builder()
                .username(generateUniqueUsername(userInfo.getName(), userInfo.getEmail()))
                .email(userInfo.getEmail())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .authProvider(AuthProvider.valueOf(registrationId.toUpperCase()))
                .providerId(userInfo.getId())
                .emailVerified(true) // OAuth2 emails are typically verified
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        // Assign default role for OAuth2 users
        Role defaultRole = roleRepository.findByName("ROLE_CONTRACTOR")
                .orElseThrow(() -> new RuntimeException("Default OAuth2 role ROLE_CONTRACTOR not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        // Log OAuth2 registration
        auditService.logUserRegistration(savedUser);

        log.info("New OAuth2 user registered: {} via {}", savedUser.getEmail(), savedUser.getAuthProvider());

        return savedUser;
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo userInfo) {
        existingUser.setFirstName(userInfo.getFirstName());
        existingUser.setLastName(userInfo.getLastName());

        User updatedUser = userRepository.save(existingUser);

        log.info("Existing OAuth2 user updated: {}", updatedUser.getEmail());

        return updatedUser;
    }

    private String generateUniqueUsername(String name, String email) {
        String baseUsername = name != null ? name.toLowerCase().replaceAll("[^a-zA-Z0-9]", "")
                : email.split("@")[0].toLowerCase();

        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

    private String fetchGithubEmail(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(
            "https://api.github.com/user/emails",
            HttpMethod.GET,
            entity,
            List.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            List<Map<String, Object>> emails = response.getBody();
            if (emails != null) {
                for (Map<String, Object> mail : emails) {
                    Boolean primary = (Boolean) mail.get("primary");
                    Boolean verified = (Boolean) mail.get("verified");
                    String email = (String) mail.get("email");
                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified) && email != null) {
                        return email;
                    }
                }
            }
        }
        return null;
    }
}
