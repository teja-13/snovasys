package com.ledgerline.admin;

import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.AuditLog;
import com.ledgerline.domain.RoleName;
import com.ledgerline.repository.AppUserRepository;
import com.ledgerline.repository.AuditLogRepository;
import com.ledgerline.repository.RoleRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            AuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AdminDtos.UserResponse createAuditor(AdminDtos.CreateAuditorRequest request, AppUser actor) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        var auditorRole = roleRepository.findByName(RoleName.AUDITOR)
                .orElseThrow(() -> new IllegalStateException("AUDITOR role is not configured"));
        AppUser user = new AppUser(request.fullName().trim(), request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.temporaryPassword()));
        user.getRoles().add(auditorRole);
        AppUser saved = userRepository.save(user);
        auditLogRepository.save(new AuditLog(actor, "USER_CREATED", "USER",
                saved.getId().toString(), "AUDITOR user created"));
        return toResponse(saved);
    }

    @Transactional
    public AdminDtos.UserResponse setActive(String email, boolean active, AppUser actor) {
        userRepository.acquireAdminMutationLock();
        AppUser target = userRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        if (!active && target.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN)
                && target.isActive()
                && userRepository.countActiveByRole(RoleName.ADMIN) <= 1) {
            throw new IllegalStateException("The last administrator cannot be deactivated");
        }
        target.setActive(active);
        auditLogRepository.save(new AuditLog(actor, active ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                "USER", target.getId().toString(), "User status changed"));
        return toResponse(target);
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.UserResponse> findUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminDtos.UserResponse assignRole(
            String email, RoleName roleName, AppUser actor) {
        userRepository.acquireAdminMutationLock();
        AppUser target = userRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        var role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role is not configured"));
        if (roleName == RoleName.CUSTOMER
                && target.getRoles().stream().anyMatch(existing -> existing.getName() == RoleName.AUDITOR)) {
            throw new IllegalStateException("An AUDITOR cannot be assigned the CUSTOMER role");
        }
        if (target.getRoles().stream().anyMatch(existing -> existing.getName() == RoleName.ADMIN)
                && roleName != RoleName.ADMIN
                && target.isActive()
                && userRepository.countActiveByRole(RoleName.ADMIN) <= 1) {
            throw new IllegalStateException("The last administrator cannot lose the ADMIN role");
        }
        target.replaceRole(role);
        auditLogRepository.save(new AuditLog(
                actor, "ROLE_ASSIGNED", "USER", target.getId().toString(),
                "Assigned role " + roleName));
        return toResponse(target);
    }

    private AdminDtos.UserResponse toResponse(AppUser user) {
        return new AdminDtos.UserResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                user.getCreatedAt(), user.isActive());
    }
}
