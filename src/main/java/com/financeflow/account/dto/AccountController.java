package com.financeflow.account;

import com.financeflow.account.dto.AccountResponse;
import com.financeflow.account.dto.CreateAccountRequest;
import com.financeflow.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Financial Accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) { this.service = service; }

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.list(principal.getId());
    }

    @GetMapping("/{id}")
    public AccountResponse get(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long id) {
        return service.get(principal.getId(), id);
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAccountRequest req) {
        return ResponseEntity.status(201).body(service.create(principal.getId(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long id) {
        service.deactivate(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}