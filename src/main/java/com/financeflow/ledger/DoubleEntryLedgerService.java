package com.financeflow.ledger;

import com.financeflow.account.AccountType;
import com.financeflow.account.FinancialAccount;
import com.financeflow.common.exception.BusinessException;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.ledger.dto.AccountBalanceSnapshot;
import com.financeflow.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class DoubleEntryLedgerService {

    private static final Set<JournalEntryStatus> FINALIZED_STATUSES =
            Set.of(JournalEntryStatus.POSTED, JournalEntryStatus.REVERSED);
    private static final Set<JournalEntryStatus> PENDING_STATUSES =
            Set.of(JournalEntryStatus.PENDING);

    private final LedgerAccountRepository ledgerAccounts;
    private final JournalEntryRepository journalEntries;
    private final LedgerPostingRepository postings;

    public DoubleEntryLedgerService(LedgerAccountRepository ledgerAccounts,
                                    JournalEntryRepository journalEntries,
                                    LedgerPostingRepository postings) {
        this.ledgerAccounts = ledgerAccounts;
        this.journalEntries = journalEntries;
        this.postings = postings;
    }

    public record PostingLine(Long ledgerAccountId, PostingSide side, BigDecimal amount, String memo) {}

    public record PostEntryCommand(
            User user,
            String idempotencyKey,
            LocalDate entryDate,
            LocalDate effectiveDate,
            String description,
            String referenceType,
            Long referenceId,
            RecordOrigin recordOrigin,
            JournalEntryStatus status,
            List<PostingLine> lines
    ) {}

    @Transactional
    public LedgerAccount ensureAccountForFinancialAccount(FinancialAccount account) {
        return ledgerAccounts.findByFinancialAccountId(account.getId())
                .orElseGet(() -> {
                    LedgerAccountClass clazz = account.getType() == AccountType.CREDIT
                            ? LedgerAccountClass.LIABILITY
                            : LedgerAccountClass.ASSET;
                    String code = clazz.name() + ":FIN_ACC:" + account.getId();
                    return ledgerAccounts.save(LedgerAccount.builder()
                            .user(account.getUser())
                            .financialAccount(account)
                            .code(code)
                            .name(account.getName())
                            .accountClass(clazz)
                            .currency(account.getCurrency() != null ? account.getCurrency() : "BRL")
                            .active(Boolean.TRUE.equals(account.getActive()))
                            .build());
                });
    }

    @Transactional
    public LedgerAccount ensureSystemAccount(User user, String code, String name,
                                             LedgerAccountClass accountClass, String currency) {
        return ledgerAccounts.findByUserIdAndCode(user.getId(), code)
                .orElseGet(() -> ledgerAccounts.save(LedgerAccount.builder()
                        .user(user)
                        .code(code)
                        .name(name)
                        .accountClass(accountClass)
                        .currency(currency != null ? currency : "BRL")
                        .active(true)
                        .build()));
    }

    @Transactional
    public JournalEntry postEntry(PostEntryCommand cmd) {
        Objects.requireNonNull(cmd.user(), "Usuário é obrigatório");
        if (cmd.idempotencyKey() == null || cmd.idempotencyKey().isBlank()) {
            throw new BusinessException("Chave de idempotência é obrigatória para lançamentos contábeis");
        }

        var existing = journalEntries.findByUserIdAndIdempotencyKey(cmd.user().getId(), cmd.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        if (cmd.lines() == null || cmd.lines().size() < 2) {
            throw new BusinessException("Um lançamento de partidas dobradas exige ao menos duas partidas");
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        String entryCurrency = null;

        JournalEntry entry = JournalEntry.builder()
                .user(cmd.user())
                .idempotencyKey(cmd.idempotencyKey().trim())
                .entryDate(cmd.entryDate() != null ? cmd.entryDate() : LocalDate.now())
                .effectiveDate(cmd.effectiveDate() != null ? cmd.effectiveDate() : LocalDate.now())
                .description(cmd.description())
                .referenceType(cmd.referenceType() != null ? cmd.referenceType() : "MANUAL")
                .referenceId(cmd.referenceId())
                .recordOrigin(cmd.recordOrigin() != null ? cmd.recordOrigin() : RecordOrigin.INTERNAL)
                .status(cmd.status() != null ? cmd.status() : JournalEntryStatus.POSTED)
                .build();

        for (PostingLine line : cmd.lines()) {
            if (line.amount() == null || line.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("O valor de cada partida contábil deve ser estritamente positivo");
            }

            LedgerAccount account = ledgerAccounts.findByIdAndUserId(line.ledgerAccountId(), cmd.user().getId())
                    .orElseThrow(() -> new NotFoundException("Conta contábil não encontrada: " + line.ledgerAccountId()));

            if (!Boolean.TRUE.equals(account.getActive())) {
                throw new BusinessException("Conta contábil inativa não aceita lançamentos: " + account.getCode());
            }

            if (entryCurrency == null) {
                entryCurrency = account.getCurrency();
            } else if (!entryCurrency.equals(account.getCurrency())) {
                throw new BusinessException("Lançamento multi-moeda direto não permitido sem conta de câmbio intermediária");
            }

            BigDecimal normalized = line.amount().setScale(4, RoundingMode.HALF_EVEN);
            if (line.side() == PostingSide.DEBIT) {
                totalDebits = totalDebits.add(normalized);
            } else {
                totalCredits = totalCredits.add(normalized);
            }

            entry.addPosting(LedgerPosting.builder()
                    .ledgerAccount(account)
                    .side(line.side())
                    .amount(normalized)
                    .currency(account.getCurrency())
                    .memo(line.memo())
                    .build());
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BusinessException(
                    "Lançamento desbalanceado: total de débitos (" + totalDebits + ") difere de créditos (" + totalCredits + ")"
            );
        }

        return journalEntries.save(entry);
    }

    @Transactional
    public JournalEntry confirmPendingEntry(Long userId, Long entryId) {
        JournalEntry entry = journalEntries.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new NotFoundException("Lançamento contábil não encontrado"));

        if (entry.getStatus() != JournalEntryStatus.PENDING) {
            throw new BusinessException("Apenas lançamentos pendentes (PENDING) podem ser confirmados");
        }

        entry.setStatus(JournalEntryStatus.POSTED);
        return journalEntries.save(entry);
    }

    @Transactional
    public JournalEntry reverseEntry(Long userId, Long entryId, String reversalIdempotencyKey, String reason) {
        if (reversalIdempotencyKey == null || reversalIdempotencyKey.isBlank()) {
            throw new BusinessException("Chave de idempotência do estorno é obrigatória");
        }

        var existingReversal = journalEntries.findByUserIdAndIdempotencyKey(userId, reversalIdempotencyKey);
        if (existingReversal.isPresent()) {
            return existingReversal.get();
        }

        JournalEntry original = journalEntries.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new NotFoundException("Lançamento contábil não encontrado"));

        if (original.getStatus() != JournalEntryStatus.POSTED) {
            throw new BusinessException("Somente lançamentos contabilizados (POSTED) podem ser estornados");
        }

        original.setStatus(JournalEntryStatus.REVERSED);
        journalEntries.save(original);

        String desc = (reason != null && !reason.isBlank())
                ? "Estorno: " + reason + " (Ref #" + original.getId() + ")"
                : "Estorno: " + original.getDescription() + " (Ref #" + original.getId() + ")";

        JournalEntry reversal = JournalEntry.builder()
                .user(original.getUser())
                .idempotencyKey(reversalIdempotencyKey.trim())
                .entryDate(LocalDate.now())
                .effectiveDate(LocalDate.now())
                .description(desc)
                .referenceType("REVERSAL")
                .referenceId(original.getId())
                .recordOrigin(RecordOrigin.INTERNAL)
                .status(JournalEntryStatus.POSTED)
                .reversalOf(original)
                .build();

        for (LedgerPosting p : original.getPostings()) {
            reversal.addPosting(LedgerPosting.builder()
                    .ledgerAccount(p.getLedgerAccount())
                    .side(p.getSide().opposite())
                    .amount(p.getAmount())
                    .currency(p.getCurrency())
                    .memo("Estorno partida #" + p.getId())
                    .build());
        }

        return journalEntries.save(reversal);
    }

    @Transactional(readOnly = true)
    public AccountBalanceSnapshot getAccountBalance(Long userId, Long ledgerAccountId) {
        LedgerAccount account = ledgerAccounts.findByIdAndUserId(ledgerAccountId, userId)
                .orElseThrow(() -> new NotFoundException("Conta contábil não encontrada"));

        BigDecimal postedDebits = postings.sumByAccountAndSideAndStatuses(ledgerAccountId, PostingSide.DEBIT, FINALIZED_STATUSES);
        BigDecimal postedCredits = postings.sumByAccountAndSideAndStatuses(ledgerAccountId, PostingSide.CREDIT, FINALIZED_STATUSES);

        BigDecimal pendingDebits = postings.sumByAccountAndSideAndStatuses(ledgerAccountId, PostingSide.DEBIT, PENDING_STATUSES);
        BigDecimal pendingCredits = postings.sumByAccountAndSideAndStatuses(ledgerAccountId, PostingSide.CREDIT, PENDING_STATUSES);

        BigDecimal booked;
        BigDecimal available;
        BigDecimal projected;

        if (account.getAccountClass().isDebitNormal()) {
            booked = postedDebits.subtract(postedCredits);
            available = booked.subtract(pendingCredits);
            projected = booked.add(pendingDebits).subtract(pendingCredits);
        } else {
            booked = postedCredits.subtract(postedDebits);
            available = booked.subtract(pendingDebits);
            projected = booked.add(pendingCredits).subtract(pendingDebits);
        }

        return new AccountBalanceSnapshot(
                account.getId(),
                account.getCode(),
                account.getCurrency(),
                booked.setScale(2, RoundingMode.HALF_EVEN),
                available.setScale(2, RoundingMode.HALF_EVEN),
                projected.setScale(2, RoundingMode.HALF_EVEN)
        );
    }
}
