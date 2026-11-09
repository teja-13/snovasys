package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.RefreshToken;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthenticationSecurityTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void passwordsAreStoredAsOneWayBcryptHashes() {
        String password = "correct horse battery staple";
        String hash = passwordEncoder.encode(password);

        assertNotEquals(password, hash);
        assertTrue(passwordEncoder.matches(password, hash));
        assertFalse(passwordEncoder.matches("wrong password", hash));
    }

    @Test
    void refreshTokenBecomesUnusableAfterRevocation() {
        AppUser user = new AppUser("Test User", "test@example.com", "hash");
        RefreshToken token = new RefreshToken(
                user, "token-hash", Instant.now().plusSeconds(3600));

        assertTrue(token.isUsableAt(Instant.now()));
        token.revoke(Instant.now());
        assertFalse(token.isUsableAt(Instant.now()));
    }
}
