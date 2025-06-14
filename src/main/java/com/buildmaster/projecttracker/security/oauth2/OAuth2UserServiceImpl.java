package com.buildmaster.projecttracker.security.oauth2;

import com.buildmaster.projecttracker.enums.AuthProvider;
import com.buildmaster.projecttracker.entity.User;
import com.buildmaster.projecttracker.repository.UserRepository;
import com.buildmaster.projecttracker.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = null;
        String username = null;

        // Extract email and username from OAuth2 provider
        if (attributes.containsKey("email")) {
            email = (String) attributes.get("email");
        }
        if (attributes.containsKey("login")) { // GitHub
            username = (String) attributes.get("login");
        } else if (attributes.containsKey("name")) { // Google
            username = (String) attributes.get("name");
        }

        // Check if user exists
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isEmpty()) {
            // Create new user with CONTRACTOR role
            user = new User();
            user.setEmail(email);
            user.setUsername(username != null ? username : email);
            // Fix: set role as Role entity if User.role expects Role
            com.buildmaster.projecttracker.entity.Role contractorRole = new com.buildmaster.projecttracker.entity.Role();
            contractorRole.setName("CONTRACTOR");
            user.setRole(contractorRole);
            user.setProvider(AuthProvider.valueOf(registrationId.toUpperCase()));
            userRepository.save(user);
        } else {
            user = userOptional.get();
            // Update user info if needed
            user.setProvider(AuthProvider.valueOf(registrationId.toUpperCase()));
            userRepository.save(user);
        }

        // Optionally, return a custom OAuth2User implementation
        return oAuth2User;
    }
}