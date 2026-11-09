package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ledgerline.admin.AdminService;
import com.ledgerline.admin.AdminDtos;
import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.Role;
import com.ledgerline.domain.RoleName;
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
class AdminRoleIntegrationTest {

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
    void adminRoleAssignmentAddsOnlyTheRequestedRole() {
        AppUser actor = userRepository.save(new AppUser(
                "Administrator", "admin-" + UUID.randomUUID() + "@example.com", "hash"));
        AppUser target = userRepository.save(new AppUser(
                "Audited User", "target-" + UUID.randomUUID() + "@example.com", "hash"));

        AdminDtos.UserResponse response = adminService.assignRole(
                target.getEmail(), RoleName.AUDITOR, actor);

        assertTrue(response.roles().contains(RoleName.AUDITOR));
        assertTrue(userRepository.findById(target.getId()).orElseThrow().getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.AUDITOR));
    }

    @Test
    void auditorCannotBeAssignedCustomerRole() {
        AppUser actor = userRepository.save(new AppUser(
                "Administrator", "admin-" + UUID.randomUUID() + "@example.com", "hash"));
        AppUser target = userRepository.save(new AppUser(
                "Auditor", "auditor-" + UUID.randomUUID() + "@example.com", "hash"));
        target.getRoles().add(roleRepository.findByName(RoleName.AUDITOR).orElseThrow());
        userRepository.save(target);

        assertThrows(IllegalStateException.class,
                () -> adminService.assignRole(target.getEmail(), RoleName.CUSTOMER, actor));
    }

    @Test
    void assignedRolesAreMutuallyExclusive() {
        AppUser actor = userRepository.save(new AppUser(
                "Administrator", "admin-" + UUID.randomUUID() + "@example.com", "hash"));
        AppUser target = userRepository.save(new AppUser(
                "Customer", "customer-" + UUID.randomUUID() + "@example.com", "hash"));
        target.getRoles().add(roleRepository.findByName(RoleName.CUSTOMER).orElseThrow());
        userRepository.save(target);

        AdminDtos.UserResponse response =
                adminService.assignRole(target.getEmail(), RoleName.AUDITOR, actor);

        assertTrue(response.roles().contains(RoleName.AUDITOR));
        assertTrue(response.roles().stream().noneMatch(role -> role == RoleName.CUSTOMER));
    }
}
