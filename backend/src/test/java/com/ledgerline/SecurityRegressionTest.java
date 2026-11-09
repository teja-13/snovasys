package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ledgerline.auth.AdminBootstrap;
import com.ledgerline.auth.AuthService;
import com.ledgerline.auth.SecurityConfig;
import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.RefreshToken;
import com.ledgerline.account.AccountService;
import com.ledgerline.auth.JwtService;
import com.ledgerline.repository.RefreshTokenRepository;
import com.ledgerline.domain.RoleName;
import com.ledgerline.repository.AppUserRepository;
import com.ledgerline.repository.RoleRepository;
import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;

class SecurityRegressionTest {

    @Test
    void inactiveJwtIsRejectedByTheAuthenticationConverter() {
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser inactive = new AppUser("Inactive", "inactive@example.com", "hash");
        inactive.setActive(false);
        UUID userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.of(inactive));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("roles", java.util.List.of("CUSTOMER"))
                .build();

        assertThrows(BadCredentialsException.class,
                () -> new SecurityConfig().jwtAuthenticationConverter(users).convert(jwt));
        verify(users).findByIdWithRoles(userId);
    }

    @Test
    void jwtAuthoritiesComeFromCurrentDatabaseRoles() {
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser customer = new AppUser("Customer", "customer@example.com", "hash");
        customer.getRoles().add(new com.ledgerline.domain.Role(RoleName.CUSTOMER));
        UUID userId = UUID.randomUUID();
        when(users.findByIdWithRoles(userId)).thenReturn(Optional.of(customer));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("roles", java.util.List.of("ADMIN"))
                .build();

        Authentication authentication = new SecurityConfig().jwtAuthenticationConverter(users).convert(jwt);

        org.junit.jupiter.api.Assertions.assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER")));
        org.junit.jupiter.api.Assertions.assertTrue(authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void jwtWithoutSubjectIsRejectedAsBadCredentials() {
        AppUserRepository users = mock(AppUserRepository.class);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThrows(BadCredentialsException.class,
                () -> new SecurityConfig().jwtAuthenticationConverter(users).convert(jwt));
    }

    @Test
    void bootstrapAcquiresDatabaseLockBeforeCheckingForAnAdmin() {
        AppUserRepository users = mock(AppUserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        when(users.countByRole(RoleName.ADMIN)).thenReturn(1L);
        AdminBootstrap bootstrap = new AdminBootstrap(
                users, roles, new BCryptPasswordEncoder(), "Admin", "admin@example.com", "temporary-password");

        bootstrap.run();

        InOrder order = inOrder(users);
        order.verify(users).acquireAdminMutationLock();
        order.verify(users).countByRole(RoleName.ADMIN);
        verify(users, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void inactiveUserCannotRefreshAnExistingToken() {
        AppUserRepository users = mock(AppUserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
        AppUser inactive = new AppUser("Inactive", "inactive-refresh@example.com", "hash");
        inactive.setActive(false);
        RefreshToken refreshToken = new RefreshToken(
                inactive, "hashed-token", Instant.now().plusSeconds(600));
        when(refreshTokens.findByTokenHash(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(refreshToken));

        AuthService authService = new AuthService(
                users, roles, refreshTokens, new BCryptPasswordEncoder(),
                mock(AuthenticationManager.class), mock(JwtService.class),
                mock(AccountService.class), Duration.ofDays(7));

        assertThrows(IllegalArgumentException.class, () -> authService.refresh("raw-token"));
    }
}
