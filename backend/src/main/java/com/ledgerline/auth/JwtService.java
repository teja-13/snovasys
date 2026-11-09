package com.ledgerline.auth;

import com.ledgerline.domain.AppUser;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final Duration accessTokenTtl;

    public JwtService(
            @Value("${spring.security.jwt.secret}") String secret,
            @Value("${spring.security.jwt.access-token-ttl}") Duration accessTokenTtl) {
        SecretKey key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.accessTokenTtl = accessTokenTtl;
    }

    public AccessToken issue(AppUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessTokenTtl);
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ledgerline")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("roles", roles)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AccessToken(token, expiresAt);
    }

    public record AccessToken(String value, Instant expiresAt) {
    }
}
