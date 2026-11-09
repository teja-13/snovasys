package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ledgerline.auth.AdminBootstrap;
import com.ledgerline.domain.RoleName;
import com.ledgerline.repository.AppUserRepository;
import com.ledgerline.repository.RoleRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class AdminBootstrapIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void concurrentBootstrapAttemptsCreateOnlyOneAdministrator() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String firstEmail = "bootstrap-first-" + suffix + "@example.com";
        String secondEmail = "bootstrap-second-" + suffix + "@example.com";
        AdminBootstrap first = new AdminBootstrap(
                userRepository, roleRepository, passwordEncoder,
                "First", firstEmail, "temporary-password-one");
        AdminBootstrap second = new AdminBootstrap(
                userRepository, roleRepository, passwordEncoder,
                "Second", secondEmail, "temporary-password-two");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        var firstRun = executor.submit(() -> {
            runInTransaction(first, start);
            return null;
        });
        var secondRun = executor.submit(() -> {
            runInTransaction(second, start);
            return null;
        });
        start.countDown();
        firstRun.get();
        secondRun.get();
        executor.shutdownNow();

        assertEquals(1, userRepository.countByRole(RoleName.ADMIN));
    }

    private void runInTransaction(AdminBootstrap bootstrap, CountDownLatch start) throws InterruptedException {
        start.await();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> bootstrap.run());
    }
}
