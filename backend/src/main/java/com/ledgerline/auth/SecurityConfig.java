package com.ledgerline.auth;

import com.ledgerline.domain.AppUser;
import com.ledgerline.repository.AppUserRepository;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    UserDetailsService userDetailsService(AppUserRepository userRepository) {
        return email -> userRepository.findByEmailIgnoreCase(email)
                .map(user -> org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .disabled(!user.isActive())
                        .authorities(user.getRoles().stream()
                                .map(role -> "ROLE_" + role.getName().name())
                                .toArray(String[]::new))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.jwt.secret}") String secret) {
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(AppUserRepository userRepository) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
                String subject = jwt.getSubject();
                if (subject == null || subject.isBlank()) {
                    throw new BadCredentialsException("Authenticated user was not found");
                }
                java.util.UUID userId;
                try {
                    userId = java.util.UUID.fromString(subject);
                } catch (IllegalArgumentException exception) {
                    throw new BadCredentialsException("Authenticated user was not found", exception);
                }
                AppUser user = userRepository.findByIdWithRoles(userId)
                        .orElseThrow(() -> new BadCredentialsException("Authenticated user was not found"));
                if (!user.isActive()) {
                    throw new BadCredentialsException("User account is inactive");
                }
                return user.getRoles().stream()
                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(
                                "ROLE_" + role.getName().name()))
                        .toList();
        });
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${spring.web.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}
