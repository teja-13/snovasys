package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ledgerline.account.AccountService;
import com.ledgerline.auth.AuthDtos;
import com.ledgerline.auth.AuthService;
import com.ledgerline.domain.Role;
import com.ledgerline.domain.RoleName;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.AppUserRepository;
import com.ledgerline.repository.RoleRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RegistrationAccountIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void ensureCustomerRoleExists() {
        roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.CUSTOMER)));
    }

    @Test
    void registrationCreatesOneZeroBalanceCustomerAccount() {
        String email = "registration-" + UUID.randomUUID() + "@example.com";

        authService.register(new AuthDtos.RegisterRequest(
                "Registered Customer", email, "correct horse battery"));

        var user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        var accounts = accountRepository.findAllByOwnerIdOrderByCreatedAtAsc(user.getId());

        assertEquals(1, accounts.size());
        assertEquals("INR", accounts.getFirst().getCurrency());
        assertEquals(0, accounts.getFirst().getBalance().signum());
    }
}
