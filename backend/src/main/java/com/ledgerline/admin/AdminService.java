package com.ledgerline.admin;

import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.AuditLog;
import com.ledgerline.domain.RoleName;
import com.ledgerline.repository.AppUserRepository;
import com.ledgerline.repository.AuditLogRepository;
import com.ledgerline.repository.RoleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminService(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.UserResponse> findUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminDtos.UserResponse assignRole(
            String email, RoleName roleName, AppUser actor) {
        AppUser target = userRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        var role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role is not configured"));
        target.getRoles().add(role);
        auditLogRepository.save(new AuditLog(
                actor, "ROLE_ASSIGNED", "USER", target.getId().toString(),
                "Assigned role " + roleName));
        return toResponse(target);
    }

    private AdminDtos.UserResponse toResponse(AppUser user) {
        return new AdminDtos.UserResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                user.getCreatedAt());
    }
}
