package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ledgerline.admin.AdminService;
import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.Role;
import com.ledgerline.domain.RoleName;
import com.ledgerline.repository.AppUserRepository;
import com.ledgerline.repository.RoleRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AdminConcurrencyIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void ensureRolesExist() {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(new Role(roleName)));
        }
    }

    @Test
    void concurrentLastAdminChangesCannotRemoveTheLastActiveAdministrator() throws Exception {
        String suffix = UUID.randomUUID().toString();
        AppUser actor = userRepository.save(new AppUser(
                "Actor", "actor-" + suffix + "@example.com", "hash"));
        AppUser admin = userRepository.save(new AppUser(
                "Only Admin", "only-admin-" + suffix + "@example.com", "hash"));
        admin.getRoles().add(roleRepository.findByName(RoleName.ADMIN).orElseThrow());
        userRepository.save(admin);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Boolean> deactivate = executor.submit(() -> {
            start.await();
            return failed(() -> adminService.setActive(admin.getEmail(), false, actor));
        });
        Future<Boolean> demote = executor.submit(() -> {
            start.await();
            return failed(() -> adminService.assignRole(admin.getEmail(), RoleName.CUSTOMER, actor));
        });
        start.countDown();

        assertTrue(deactivate.get());
        assertTrue(demote.get());
        executor.shutdownNow();

        AppUser persisted = userRepository.findByIdWithRoles(admin.getId()).orElseThrow();
        assertTrue(persisted.isActive());
        assertTrue(persisted.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN));
    }

    private boolean failed(Runnable operation) {
        try {
            operation.run();
            return false;
        } catch (IllegalStateException expected) {
            return true;
        }
    }
}
