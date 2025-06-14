package com.buildmaster.projecttracker.config;

import com.buildmaster.projecttracker.security.JwtAuthenticationEntryPoint;
import com.buildmaster.projecttracker.security.JwtAuthenticationFilter;
import com.buildmaster.projecttracker.security.oauth2.CustomOAuth2UserService;
import com.buildmaster.projecttracker.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final @Lazy JwtAuthenticationFilter jwtAuthFilter; // @Lazy is fine here for filter chain order

    // Removed: private final @Lazy AuthenticationProvider authenticaticationProvider;
    // The AuthenticationProvider bean will be automatically discovered by AuthenticationManager

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // This bean provides the default authentication logic using UserDetailsService and PasswordEncoder
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // The AuthenticationManager will automatically discover all registered AuthenticationProvider beans
        // (like DaoAuthenticationProvider and OAuth2UserService)
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Publicly accessible endpoints
                        .requestMatchers("/", "/oauth2/**", "/login").permitAll()
                        .requestMatchers("/api/*/auth/register", "/api/*/auth/login").permitAll()
                        .requestMatchers("/api/*/auth/logout").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/health", "/api/v1/test").permitAll()
                        .requestMatchers("/").permitAll()

                        // Role-based authorization
                        .requestMatchers("/api/*/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/*/projects").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/*/projects").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/*/projects/*/summary").hasAnyRole("CONTRACTOR", "DEVELOPER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/*/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/*/tasks").hasRole("DEVELOPER")
                        .requestMatchers(HttpMethod.PUT, "/api/*/tasks").hasRole("DEVELOPER")

                        .requestMatchers(HttpMethod.GET, "/api/*/projects").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/*/tasks").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/*/developers").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/*/tasks/*").authenticated()
                        .requestMatchers("/api/*/users/").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(
                                authorization -> authorization.baseUri("/oauth2/authorize") // Custom base URI for OAuth2 authorization
                        )
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
        ;
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "https://localhost:*", "https://*.buildmaster.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply CORS to all paths
        return source;
    }
}
