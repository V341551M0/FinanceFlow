package com.financeflow.ledger;

import com.financeflow.common.exception.BusinessException;
import com.financeflow.ledger.DoubleEntryLedgerService.PostEntryCommand;
import com.finance

flow.ledger.DoubleEntryLedgerService.PostingLine;
import com.financeflow.ledger.dto.AccountBalanceSnapshot;
import com.financeflow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoubleEntryLedgerServiceTest {

    @Mock private LedgerAccountRepository ledgerAccounts;
    @Mock private JournalEntryRepository journalEntries;
    @Mock private LedgerPostingRepository postings;

    @InjectMocks private DoubleEntryLedgerService service;

    private User user;
    private LedgerAccount checkingAccount;
    private LedgerAccount revenueAccount;

    @BeforeEach
    void setUp() {
        user = User.builder().name("Kauã").email("kaua@financeflow.dev").passwordHash("hash").build();
        user.setId(1L);

        checkingAccount = LedgerAccount.builder()
                .user(user).code("ASSET:FIN_ACC:10").name("Conta Corrente")
                .accountClass(LedgerAccountClass.ASSET).currency("BRL").active(true).build();
        checkingAccount.setId(10L);

        revenueAccount = LedgerAccount.builder()
                .user(user).code("REVENUE:GENERAL").name("Receitas")
                .accountClass(LedgerAccountClass.REVENUE).currency("BRL").active(true).build();
        revenueAccount.setId(20L);
    }

    @Test
    void postEntry_balancedDebitAndCredit_succeeds() {
        when(journalEntries.findByUserIdAndIdempotencyKey(1L, "tx-100")).thenReturn(Optional.empty());
        when(ledgerAccounts.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(checkingAccount));
        when(ledgerAccounts.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(revenueAccount));
        when(journalEntries.save(any(JournalEntry.class))).thenAnswer(inv -> {
            JournalEntry e = inv.getArgument(0);
            e.setId(500L);
            return e;
        });

        var cmd = new PostEntryCommand(
                user, "tx-100", LocalDate.now(), LocalDate.now(),
                "Salário mensal", "TRANSACTION", 100L,
                RecordOrigin.INTERNAL, JournalEntryStatus.POSTED,
                List.of(
                        new PostingLine(10L, PostingSide.DEBIT, new BigDecimal("4500.00"), "Entrada"),
                        new PostingLine(20L, PostingSide.CREDIT, new BigDecimal("4500.00"), "Receita")
                )
        );

        JournalEntry saved = service.postEntry(cmd);

        assertThat(saved.getId()).isEqualTo(500L);
        assertThat(saved.getPostings()).hasSize(2);
        assertThat(saved.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
    }

    @Test
    void postEntry_unbalancedDebitsAndCredits_throwsBusinessException() {
        when(journalEntries.findByUserIdAndIdempotencyKey(1L, "tx-bad")).thenReturn(Optional.empty());
        when(ledgerAccounts.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(checkingAccount));
        when(ledgerAccounts.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(revenueAccount));

        var cmd = new PostEntryCommand(
                user, "tx-bad", LocalDate.now(), LocalDate.now(),
                "Lançamento inválido", "MANUAL", null,
                RecordOrigin.INTERNAL, JournalEntryStatus.POSTED,
                List.of(
                        new PostingLine(10L, PostingSide.DEBIT, new BigDecimal("100.00"), null),
                        new PostingLine(20L, PostingSide.CREDIT, new BigDecimal("99.99"), null)
                )
        );

        assertThatThrownBy(() -> service.postEntry(cmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desbalanceado");
        verify(journalEntries, never()).save(any());
    }

    @Test
    void postEntry_duplicateIdempotencyKey_returnsExistingEntryWithoutDuplicating() {
        JournalEntry existing = JournalEntry.builder()
                .id(77L).user(user).idempotencyKey("tx-dup").description("Já lançado")
                .entryDate(LocalDate.now()).effectiveDate(LocalDate.now())
                .referenceType("TRANSACTION").status(JournalEntryStatus.POSTED).build();

        when(journalEntries.findByUserIdAndIdempotencyKey(1L, "tx-dup")).thenReturn(Optional.of(existing));

        var cmd = new PostEntryCommand(
                user, "tx-dup", LocalDate.now(), LocalDate.now(),
                "Já lançado", "TRANSACTION", 77L,
                RecordOrigin.INTERNAL, JournalEntryStatus.POSTED,
                List.of(
                        new PostingLine(10L, PostingSide.DEBIT, new BigDecimal("50.00"), null),
                        new PostingLine(20L, PostingSide.CREDIT, new BigDecimal("50.00"), null)
                )
        );

        JournalEntry result = service.postEntry(cmd);

        assertThat(result.getId()).isEqualTo(77L);
        verify(journalEntries, never()).save(any());
    }

    @Test
    void reverseEntry_postedEntry_createsInvertedPostingsAndMarksOriginalReversed() {
        JournalEntry original = JournalEntry.builder()
                .id(10L).user(user).idempotencyKey("orig-1").description("Compra duplicada")
                .entryDate(LocalDate.now()).effectiveDate(LocalDate.now())
                .referenceType("TRANSACTION").status(JournalEntryStatus.POSTED).build();
        original.addPosting(LedgerPosting.builder()
                .id(1L).ledgerAccount(checkingAccount).side(PostingSide.CREDIT)
                .amount(new BigDecimal("120.0000")).currency("BRL").build());
        original.addPosting(LedgerPosting.builder()
                .id(2L).ledgerAccount(revenueAccount).side(PostingSide.DEBIT)
                .amount(new BigDecimal("120.0000")).currency("BRL").build());

        when(journalEntries.findByUserIdAndIdempotencyKey(1L, "rev-1")).thenReturn(Optional.empty());
        when(journalEntries.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(original));
        when(journalEntries.save(any(JournalEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        JournalEntry reversal = service.reverseEntry(1L, 10L, "rev-1", "Cobrança indevida");

        assertThat(original.getStatus()).isEqualTo(JournalEntryStatus.REVERSED);
        assertThat(reversal.getReversalOf()).isEqualTo(original);
        assertThat(reversal.getPostings()).hasSize(2);
        assertThat(reversal.getPostings().get(0).getSide()).isEqualTo(PostingSide.DEBIT);
        assertThat(reversal.getPostings().get(1).getSide()).isEqualTo(PostingSide.CREDIT);
    }

    @Test
    void getAccountBalance_computesBookedAvailableAndProjectedSeparately() {
        when(ledgerAccounts.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(checkingAccount));
        var finalized = Set.of(JournalEntryStatus.POSTED, JournalEntryStatus.REVERSED);
        var pending = Set.of(JournalEntryStatus.PENDING);

        when(postings.sumByAccountAndSideAndStatuses(10L, PostingSide.DEBIT, finalized))
                .thenReturn(new BigDecimal("1000.0000"));
        when(postings.sumByAccountAndSideAndStatuses(10L, PostingSide.CREDIT, finalized))
                .thenReturn(new BigDecimal("250.0000"));
        when(postings.sumByAccountAndSideAndStatuses(10L, PostingSide.DEBIT, pending))
                .thenReturn(new BigDecimal("300.0000"));
        when(postings.sumByAccountAndSideAndStatuses(10L, PostingSide.CREDIT, pending))
                .thenReturn(new BigDecimal("100.0000"));

        AccountBalanceSnapshot snap = service.getAccountBalance(1L, 10L);

        assertThat(snap.bookedBalance()).isEqualByComparingTo("750.00");
        assertThat(snap.availableBalance()).isEqualByComparingTo("650.00");
        assertThat(snap.projectedBalance()).isEqualByComparingTo("950.00");
    }
}
