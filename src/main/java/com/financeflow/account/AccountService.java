package com.financeflow.account;

import com.financeflow.account.dto.AccountResponse;
import com.financeflow.account.dto.CreateAccountRequest;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.ledger.DoubleEntryLedgerService;
import com.financeflow.ledger.DoubleEntryLedgerService.PostEntryCommand;
import com.financeflow.ledger.DoubleEntryLedgerService.PostingLine;
import com.financeflow.ledger.JournalEntryStatus;
import com.financeflow.ledger.LedgerAccount;
import com.financeflow.ledger.LedgerAccountClass;
import com.financeflow.ledger.PostingSide;
import com.financeflow.ledger.RecordOrigin;
import com.financeflow.user.User;
import com.financeflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final FinancialAccountRepository accounts;
    private final UserRepository users;
    private final DoubleEntryLedgerService ledger;

    public AccountService(FinancialAccountRepository accounts,
                          UserRepository users,
                          DoubleEntryLedgerService ledger) {
        this.accounts = accounts;
        this.users = users;
        this.ledger = ledger;
    }

    @Transactional
    public AccountResponse create(Long userId, CreateAccountRequest req) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        String currency = req.currency().toUpperCase();
        var account = FinancialAccount.builder()
                .user(user)
                .name(req.name().trim())
                .institution(req.institution().trim())
                .type(req.type())
                .currency(currency)
                .initialBalance(req.initialBalance())
                .currentBalance(req.initialBalance())
                .active(true)
                .build();

        FinancialAccount saved = accounts.save(account);
        LedgerAccount assetAcc = ledger.ensureAccountForFinancialAccount(saved);

        if (assetAcc != null && req.initialBalance() != null && req.initialBalance().compareTo(BigDecimal.ZERO) > 0) {
            LedgerAccount equityAcc = ledger.ensureSystemAccount(
                    user,
                    "EQUITY:OPENING_BALANCE:" + currency,
                    "Saldo de Abertura (" + currency + ")",
                    LedgerAccountClass.EQUITY,
                    currency
            );
            if (equityAcc != null) {
                String key = "OPENING_ACC:" + (saved.getId() != null ? saved.getId() : UUID.randomUUID());
                ledger.postEntry(new PostEntryCommand(
                        user,
                        key,
                        LocalDate.now(),
                        LocalDate.now(),
                        "Saldo inicial — " + saved.getName(),
                        "ACCOUNT_OPENING",
                        saved.getId(),
                        RecordOrigin.INTERNAL,
                        JournalEntryStatus.POSTED,
                        List.of(
                                new PostingLine(assetAcc.getId(), PostingSide.DEBIT, req.initialBalance(), "Abertura de conta"),
                                new PostingLine(equityAcc.getId(), PostingSide.CREDIT, req.initialBalance(), "Contrapartida patrimônio líquido")
                        )
                ));
            }
        }

        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(Long userId) {
        return accounts.findAllByUserIdAndActiveTrueOrderByNameAsc(userId)
                .stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long userId, Long id) {
        return accounts.findByIdAndUserId(id, userId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
    }

    @Transactional
    public void deactivate(Long userId, Long id) {
        var account = accounts.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        account.setActive(false);
    }
}
