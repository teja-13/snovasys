package com.ledgerline.account;

import com.ledgerline.domain.AccountStatus;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AppUserRepository userRepository;

    public AccountController(AccountService accountService, AppUserRepository userRepository) {
        this.accountService = accountService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<AccountDtos.AccountResponse> myAccounts(Authentication authentication) {
        return accountService.findOwnedBy(currentUser(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public AccountDtos.AccountResponse createForSelf(Authentication authentication) {
        AppUser user = currentUser(authentication);
        return accountService.createForCustomer(user.getEmail(), user);
    }

    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AccountDtos.AccountResponse createForCustomer(
            @Valid @RequestBody AccountDtos.CreateAccountRequest request,
            Authentication authentication) {
        return accountService.createForCustomer(request.ownerEmail(), currentUser(authentication));
    }

    @PutMapping("/{accountNumber}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountDtos.AccountResponse changeStatus(
            @PathVariable String accountNumber,
            @Valid @RequestBody AccountDtos.StatusRequest request,
            Authentication authentication) {
        return accountService.changeStatus(accountNumber, request.status(), currentUser(authentication));
    }

    private AppUser currentUser(Authentication authentication) {
        return userRepository.findById(java.util.UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
    }
}
