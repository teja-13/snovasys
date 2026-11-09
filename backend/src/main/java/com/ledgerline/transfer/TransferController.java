package com.ledgerline.transfer;

import com.ledgerline.domain.AppUser;
import com.ledgerline.repository.AppUserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;
    private final TransferHistoryService transferHistoryService;
    private final AppUserRepository userRepository;

    public TransferController(
            TransferService transferService,
            TransferHistoryService transferHistoryService,
            AppUserRepository userRepository) {
        this.transferService = transferService;
        this.transferHistoryService = transferHistoryService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'AUDITOR')")
    public List<TransferDtos.TransferResponse> history(Authentication authentication) {
        AppUser user = currentUser(authentication);
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER"))
                ? transferHistoryService.findForCustomer(user)
                : transferHistoryService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public TransferDtos.TransferResponse transfer(
            @Valid @RequestBody TransferDtos.CreateTransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        return transferService.transfer(request, idempotencyKey, currentUser(authentication));
    }

    private AppUser currentUser(Authentication authentication) {
        return userRepository.findById(java.util.UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
    }
}
