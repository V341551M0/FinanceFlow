package com.financeflow.transaction;

import com.financeflow.security.UserPrincipal;
import com.financeflow.transaction.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) { this.service = service; }

    @GetMapping
    public List<TransactionResponse> list(@AuthenticationPrincipal UserPrincipal p) {
        return service.listRecent(p.getId());
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody CreateTransactionRequest req) {
        return ResponseEntity.status(201).body(service.create(p.getId(), req));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody CreateTransferRequest req) {
        service.createTransfer(p.getId(), req);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal p,
                                       @PathVariable Long id) {
        service.delete(p.getId(), id);
        return ResponseEntity.noContent().build();
    }
}