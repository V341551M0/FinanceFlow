package com.financeflow.space;

import com.financeflow.account.FinancialAccountRepository;
import com.financeflow.common.exception.BusinessException;
import com.financeflow.ledger.DoubleEntryLedgerService;
import com.financeflow.ledger.JournalEntry;
import com.financeflow.ledger.LedgerAccount;
import com.financeflow.ledger.LedgerAccountClass;
import com.financeflow.space.dto.CreateSpaceRequest;
import com.financeflow.space.dto.CreateSpaceTransferRequest;
import com.financeflow.space.dto.SpaceReportResponse;
import com.financeflow.transaction.Transaction;
import com.financeflow.transaction.TransactionRepository;
import com.financeflow.transaction.TransactionStatus;
import com.financeflow.transaction.TransactionType;
import com.financeflow.user.User;
import com.financeflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialSpaceServiceTest {

    @Mock private FinancialSpaceRepository spaces;
    @Mock private SpaceTransferRepository transfers;
    @Mock private SpaceAnalyticsRepository analytics;
    @Mock private FinancialAccountRepository accounts;
    @Mock private TransactionRepository transactions;
    @Mock private UserRepository users;
    @Mock private DoubleEntryLedgerService ledger;

    @InjectMocks private FinancialSpaceService service;

    private User user;
    private FinancialSpace personalSpace;
    private FinancialSpace projectSpace;

    @BeforeEach
    void setUp() {
        user = User.builder().name("Kauã").email("kaua@financeflow.dev").passwordHash("hash").currency("BRL").build();
        user.setId(1L);

        personalSpace = FinancialSpace.builder()
                .user(user).name("Finanças Pessoais").slug("pessoal")
                .type(SpaceType.PERSONAL).currency("BRL").defaultSpace(true).active(true).build();
        personalSpace.setId(10L);

        projectSpace = FinancialSpace.builder()
                .user(user).name("Projeto SaaS").slug("projeto-saas")
                .type(SpaceType.SOFTWARE_PROJECT).currency("BRL").defaultSpace(false).active(true).build();
        projectSpace.setId(20L);
    }

    @Test
    void create_projectSpace_generatesNormalizedSlug() {
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(spaces.findByUserIdAndDefaultSpaceTrue(1L)).thenReturn(Optional.of(personalSpace));
        when(spaces.existsByUserIdAndSlug(1L, "loja-virtual-tech")).thenReturn(false);
        when(spaces.save(any(FinancialSpace.class))).thenAnswer(inv -> {
            FinancialSpace s = inv.getArgument(0);
            s.setId(30L);
            return s;
        });

        var req = new CreateSpaceRequest(
                "Loja Virtual Tech", SpaceType.ONLINE_STORE,
                "Operação e-commerce", "BRL", "#4EBE96", "store", null
        );

        var res = service.create(1L, req);

        assertThat(res.id()).isEqualTo(30L);
        assertThat(res.slug()).isEqualTo("loja-virtual-tech");
        assertThat(res.type()).isEqualTo(SpaceType.ONLINE_STORE);
        assertThat(res.defaultSpace()).isFalse();
    }

    @Test
    void archive_defaultPersonalSpace_throwsBusinessException() {
        when(spaces.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(personalSpace));

        assertThatThrownBy(() -> service.archive(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("padrão");
        verify(spaces, never()).save(any());
    }

    @Test
    void transferBetweenSpaces_postsDoubleEntryJournalWithoutDuplicatingTransactions() {
        when(spaces.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(personalSpace));
        when(spaces.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(projectSpace));
        when(users.findById(1L)).thenReturn(Optional.of(user));

        var srcEq = LedgerAccount.builder().code("EQUITY:SPACE:10:BRL").accountClass(LedgerAccountClass.EQUITY).build();
        srcEq.setId(101L);
        var dstEq = LedgerAccount.builder().code("EQUITY:SPACE:20:BRL").accountClass(LedgerAccountClass.EQUITY).build();
        dstEq.setId(102L);

        when(ledger.ensureSystemAccount(eq(user), eq("EQUITY:SPACE:10:BRL"), any(), eq(LedgerAccountClass.EQUITY), eq("BRL")))
                .thenReturn(srcEq);
        when(ledger.ensureSystemAccount(eq(user), eq("EQUITY:SPACE:20:BRL"), any(), eq(LedgerAccountClass.EQUITY), eq("BRL")))
                .thenReturn(dstEq);
        when(ledger.postEntry(any())).thenReturn(JournalEntry.builder().id(900L).build());
        when(transfers.save(any(SpaceTransfer.class))).thenAnswer(inv -> {
            SpaceTransfer st = inv.getArgument(0);
            st.setId(40L);
            return st;
        });

        var req = new CreateSpaceTransferRequest(
                10L, 20L, new BigDecimal("2500.00"), LocalDate.now(), "Aporte inicial servidor", "aporte-1"
        );

        var res = service.transferBetweenSpaces(1L, req);

        assertThat(res.id()).isEqualTo(40L);
        assertThat(res.journalEntryId()).isEqualTo(900L);
        assertThat(res.amount()).isEqualByComparingTo("2500.00");
        verify(ledger).postEntry(any());
        verifyNoInteractions(transactions);
    }

    @Test
    void getSpaceReport_computesOperatingResultProfitMarginAndNetCashFlow() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);

        when(spaces.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(projectSpace));
        when(analytics.sumBySpaceAndType(1L, 20L, TransactionType.INCOME, TransactionStatus.POSTED, from, to))
                .thenReturn(new BigDecimal("10000.00"));
        when(analytics.sumBySpaceAndType(1L, 20L, TransactionType.EXPENSE, TransactionStatus.POSTED, from, to))
                .thenReturn(new BigDecimal("2500.00"));
        when(transfers.sumIncomingForSpace(1L, 20L, from, to)).thenReturn(new BigDecimal("1000.00"));
        when(transfers.sumOutgoingForSpace(1L, 20L, from, to)).thenReturn(new BigDecimal("4000.00"));
        when(analytics.countTransactionsInPeriod(1L, 20L, from, to)).thenReturn(18L);
        when(analytics.countActiveAccounts(1L, 20L)).thenReturn(1L);
        when(analytics.countActiveGoals(1L, 20L)).thenReturn(2L);

        SpaceReportResponse report = service.getSpaceReport(1L, 20L, from, to);

        assertThat(report.operatingResult()).isEqualByComparingTo("7500.00");
        assertThat(report.profitMarginPercent()).isEqualByComparingTo("75.00");
        assertThat(report.netSpaceCashFlow()).isEqualByComparingTo("4500.00");
        assertThat(report.transactionCount()).isEqualTo(18L);
    }

    @Test
    void allocateTransaction_reassignsTransactionAndPairWithoutDuplication() {
        Transaction tx = Transaction.builder().user(user).transferPairId(99L).build();
        tx.setId(98L);
        Transaction pair = Transaction.builder().user(user).transferPairId(98L).build();
        pair.setId(99L);

        when(transactions.findByIdAndUserId(98L, 1L)).thenReturn(Optional.of(tx));
        when(transactions.findByIdAndUserId(99L, 1L)).thenReturn(Optional.of(pair));
        when(spaces.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(projectSpace));

        service.allocateTransaction(1L, 98L, 20L);

        assertThat(tx.getSpace()).isEqualTo(projectSpace);
        assertThat(pair.getSpace()).isEqualTo(projectSpace);
        verify(transactions, times(2)).save(any(Transaction.class));
    }
}
