package com.ledgerline.auth;

import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.RefreshToken;
import com.ledgerline.domain.Role;
import com.ledgerline.domain.RoleName;
import com.ledgerline.account.AccountService;
import com.ledgerline.repository.AppUserRepository;
import com.ledgerline.repository.RefreshTokenRepository;
import com.ledgerline.repository.RoleRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AccountService accountService;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AccountService accountService,
            @Value("${spring.security.jwt.refresh-token-ttl}") Duration refreshTokenTtl) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accountService = accountService;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public AuthDtos.TokenResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role is not configured"));
        AppUser user = new AppUser(
                request.fullName().trim(),
                request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.password()));
        user.getRoles().add(customerRole);
        AppUser savedUser = userRepository.save(user);
        accountService.createForCustomer(savedUser.getEmail(), savedUser);
        return issueTokens(savedUser);
    }

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.email().trim().toLowerCase(), request.password()));
        AppUser user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
        return issueTokens(user);
    }

    @Transactional
    public AuthDtos.TokenResponse refresh(String rawToken) {
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> token.isUsableAt(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is invalid or expired"));
        oldToken.revoke(Instant.now());
        return issueTokens(oldToken.getUser());
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    private AuthDtos.TokenResponse issueTokens(AppUser user) {
        JwtService.AccessToken accessToken = jwtService.issue(user);
        String rawRefreshToken = randomToken();
        Instant refreshExpiresAt = Instant.now().plus(refreshTokenTtl);
        refreshTokenRepository.save(new RefreshToken(user, hash(rawRefreshToken), refreshExpiresAt));
        return new AuthDtos.TokenResponse(
                accessToken.value(), rawRefreshToken, accessToken.expiresAt(), refreshExpiresAt);
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
