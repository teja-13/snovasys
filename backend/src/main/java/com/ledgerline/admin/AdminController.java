package com.ledgerline.admin;

import com.ledgerline.domain.AppUser;
import com.ledgerline.repository.AppUserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AppUserRepository userRepository;

    public AdminController(AdminService adminService, AppUserRepository userRepository) {
        this.adminService = adminService;
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<AdminDtos.UserResponse> users() {
        return adminService.findUsers();
    }

    @PostMapping("/users/{email}/roles")
    @ResponseStatus(HttpStatus.OK)
    public AdminDtos.UserResponse assignRole(
            @PathVariable String email,
            @Valid @RequestBody AdminDtos.AssignRoleRequest request,
            Authentication authentication) {
        return adminService.assignRole(
                email, request.role(), currentUser(authentication));
    }

    @PostMapping("/auditors")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminDtos.UserResponse createAuditor(
            @Valid @RequestBody AdminDtos.CreateAuditorRequest request,
            Authentication authentication) {
        return adminService.createAuditor(request, currentUser(authentication));
    }

    @PostMapping("/users/{email}/active")
    public AdminDtos.UserResponse setActive(
            @PathVariable String email,
            @Valid @RequestBody AdminDtos.ActiveRequest request,
            Authentication authentication) {
        return adminService.setActive(email, request.active(), currentUser(authentication));
    }

    private AppUser currentUser(Authentication authentication) {
        return userRepository.findById(java.util.UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
    }
}
