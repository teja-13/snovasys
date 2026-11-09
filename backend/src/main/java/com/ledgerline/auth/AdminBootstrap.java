package com.ledgerline.auth;

import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.RoleName;
import com.ledgerline.repository.AppUserRepository;
import com.ledgerline.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String fullName;
    private final String email;
    private final String password;

    public AdminBootstrap(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ledgerline.bootstrap-admin.full-name:}") String fullName,
            @Value("${ledgerline.bootstrap-admin.email:}") String email,
            @Value("${ledgerline.bootstrap-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        userRepository.acquireAdminMutationLock();
        if (userRepository.countByRole(RoleName.ADMIN) > 0) {
            return;
        }
        var role = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role is not configured"));
        AppUser admin = new AppUser(fullName.isBlank() ? "LedgerLine Administrator" : fullName.trim(),
                email.trim().toLowerCase(), passwordEncoder.encode(password));
        admin.getRoles().add(role);
        userRepository.save(admin);
    }
}
