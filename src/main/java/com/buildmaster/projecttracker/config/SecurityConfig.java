package com.buildmaster.projecttracker.config;

import com.buildmaster.projecttracker.security.JwtAuthenticationEntryPoint;
import com.buildmaster.projecttracker.security.JwtAuthenticationFilter;
import com.buildmaster.projecttracker.security.oauth2.CustomOAuth2UserService;
import com.buildmaster.projecttracker.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                // Remove custom JWT entry point for browser-based login flows
                // .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/oauth2/**", "/login").permitAll()
                        // Public endpoints
                        .requestMatchers("/api/*/auth/register", "/api/*/auth/login").permitAll()
                        .requestMatchers("/api/*/auth/logout").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/health", "/api/v1/test").permitAll()
                        .requestMatchers("/").permitAll() // Allow public access to the home endpoint

                        // H2 Console (only for development)
                        .requestMatchers("/h2-console/**").hasRole("ADMIN")

                        // Admin-only endpoints
                        .requestMatchers("/api/*/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")

                        // Manager and Admin can create/update projects
                        .requestMatchers(HttpMethod.POST, "/api/*/projects").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/*/projects").hasAnyRole("MANAGER", "ADMIN")

                        // Manager and Admin can create tasks
                        .requestMatchers(HttpMethod.POST, "/api/*/tasks").hasAnyRole("MANAGER", "ADMIN")

                        // Contractors have read-only access to project summaries
                        .requestMatchers(HttpMethod.GET, "/api/*/projects/*/summary").hasAnyRole("CONTRACTOR", "DEVELOPER", "MANAGER", "ADMIN")

                        // All authenticated users can read basic project info
                        .requestMatchers(HttpMethod.GET, "/api/*/projects").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/*/tasks").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/*/developers").authenticated()

                        // Task updates - handled by method-level security
                        .requestMatchers(HttpMethod.PUT, "/api/*/tasks").authenticated()

                        // User profile endpoints
                        .requestMatchers("/api/*/users/me").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                );

        // REMOVE the JWT filter for browser login redirect to work:
        // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Allow H2 console frames (development only)
        http.headers(headers -> headers.frameOptions().sameOrigin());

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
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}