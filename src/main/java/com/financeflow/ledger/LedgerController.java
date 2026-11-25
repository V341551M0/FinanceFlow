package com.financeflow.ledger;

import com.financeflow.ledger.dto.AccountBalanceSnapshot;
import com.financeflow.ledger.dto.LedgerAccountSummaryResponse;
import com.financeflow.ledger.dto.LedgerEntryResponse;
import com.financeflow.ledger.dto.ReverseEntryRequest;
import com.financeflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final DoubleEntryLedgerService ledger;

    public LedgerController(DoubleEntryLedgerService ledger) {
        this.ledger = ledger;
    }

    @GetMapping("/accounts")
    public List<LedgerAccountSummaryResponse> listAccounts(@AuthenticationPrincipal UserPrincipal p) {
        return ledger.listAccountsWithBalances(p.getId());
    }

    @GetMapping("/accounts/{id}/balance")
    public AccountBalanceSnapshot getAccountBalance(@AuthenticationPrincipal UserPrincipal p,
                                                    @PathVariable Long id) {
        return ledger.getAccountBalance(p.getId(), id);
    }

    @GetMapping("/entries")
    public List<LedgerEntryResponse> listEntries(@AuthenticationPrincipal UserPrincipal p) {
        return ledger.listEntries(p.getId());
    }

    @PostMapping("/entries/{id}/confirm")
    public LedgerEntryResponse confirmEntry(@AuthenticationPrincipal UserPrincipal p,
                                            @PathVariable Long id) {
        return LedgerEntryResponse.from(ledger.confirmPendingEntry(p.getId(), id));
    }

    @PostMapping("/entries/{id}/reverse")
    public LedgerEntryResponse reverseEntry(@AuthenticationPrincipal UserPrincipal p,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ReverseEntryRequest req) {
        return LedgerEntryResponse.from(
                ledger.reverseEntry(p.getId(), id, req.idempotencyKey(), req.reason())
        );
    }
}
