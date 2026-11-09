package com.ledgerline.deposit;

import com.ledgerline.domain.AppUser;
import com.ledgerline.repository.AppUserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deposits")
public class DepositController {

    private final DepositService depositService;
    private final AppUserRepository userRepository;

    public DepositController(DepositService depositService, AppUserRepository userRepository) {
        this.depositService = depositService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public DepositDtos.DepositResponse deposit(
            @Valid @RequestBody DepositDtos.CreateDepositRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        return depositService.deposit(request, idempotencyKey, currentUser(authentication));
    }

    private AppUser currentUser(Authentication authentication) {
        return userRepository.findById(java.util.UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
    }
}
