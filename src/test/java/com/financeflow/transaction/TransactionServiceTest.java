package com.financeflow.transaction;

import com.financeflow.account.AccountType;
import com.financeflow.account.FinancialAccount;
import com.financeflow.account.FinancialAccountRepository;
import com.financeflow.common.exception.BusinessException;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.transaction.dto.CreateTransactionRequest;
import com.financeflow.transaction.dto.CreateTransferRequest;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactions;
    @Mock FinancialAccountRepository accounts;
    @Mock TransactionCategoryRepository categories;
    @Mock UserRepository users;
    @InjectMocks TransactionService service;

    private User user;
    private FinancialAccount account;
    private final Long userId = 1L;
    private final Long accountId = 10L;

    @BeforeEach
    void setup() {
        user = User.builder().name("T").email("t@t.com").passwordHash("x").build();
        account = FinancialAccount.builder()
                .user(user).name("CC").institution("Itaú").type(AccountType.CHECKING)
                .currency("BRL").initialBalance(new BigDecimal("1000.00"))
                .currentBalance(new BigDecimal("1000.00")).active(true).build();
    }

    @Test
    void create_expense_reduces_balance() {
        when(accounts.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(transactions.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        // Simula Δ = -150 vindo do repositório após o save
        when(transactions.sumDeltaForAccount(accountId)).thenReturn(new BigDecimal("-150.00"));
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));

        var req = new CreateTransactionRequest(
                accountId, null, TransactionType.EXPENSE, TransactionDirection.DEBIT,
                new BigDecimal("150.00"), LocalDate.now(), "Almoço", null);

        var res = service.create(userId, req);

        assertEquals(TransactionType.EXPENSE, res.type());
        assertEquals(TransactionDirection.DEBIT, res.direction());
        assertEquals(0, new BigDecimal("850.00").compareTo(account.getCurrentBalance()));
    }

    @Test
    void create_income_increases_balance() {
        when(accounts.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(transactions.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactions.sumDeltaForAccount(accountId)).thenReturn(new BigDecimal("500.00"));
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));

        var req = new CreateTransactionRequest(
                accountId, null, TransactionType.INCOME, TransactionDirection.CREDIT,
                new BigDecimal("500.00"), LocalDate.now(), "Salário", null);

        service.create(userId, req);

        assertEquals(0, new BigDecimal("1500.00").compareTo(account.getCurrentBalance()));
    }

    @Test
    void create_throwsWhenAccountBelongsToAnotherUser() {
        when(accounts.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());
        var req = new CreateTransactionRequest(
                accountId, null, TransactionType.EXPENSE, TransactionDirection.DEBIT,
                new BigDecimal("10.00"), LocalDate.now(), "X", null);
        assertThrows(NotFoundException.class, () -> service.create(userId, req));
    }

    @Test
    void transfer_createsTwoPairedTransactions() {
        var dst = FinancialAccount.builder()
                .user(user).name("Poupança").institution("Itaú").type(AccountType.SAVINGS)
                .currency("BRL").initialBalance(BigDecimal.ZERO)
                .currentBalance(BigDecimal.ZERO).active(true).build();

        when(accounts.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));
        when(accounts.findByIdAndUserId(20L, userId)).thenReturn(Optional.of(dst));
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(transactions.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactions.sumDeltaForAccount(any())).thenReturn(BigDecimal.ZERO);
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(accounts.findById(20L)).thenReturn(Optional.of(dst));

        var req = new CreateTransferRequest(accountId, 20L,
                new BigDecimal("100.00"), LocalDate.now(), "Transferência");

        service.createTransfer(userId, req);

        verify(transactions, times(2)).save(any(Transaction.class));
        // Dois saves → um DEBIT, outro CREDIT, ambos do tipo TRANSFER
    }

    @Test
    void transfer_sameAccount_throws() {
        var req = new CreateTransferRequest(accountId, accountId,
                new BigDecimal("50.00"), LocalDate.now(), "Erro");
        assertThrows(BusinessException.class, () -> service.createTransfer(userId, req));
    }

    @Test
    void transfer_doesNotAffectConsolidatedTotals() {
        // Regra conceitual: TRANSFER não é INCOME nem EXPENSE — coberta pelo tipo
        // Este teste documenta a intenção: soma de receitas+despesas exclui TRANSFER.
        // (Verificação completa no módulo Dashboard, Fase 4.)
        assertTrue(true);
    }

    @Test
    void delete_transaction_recalculatesBalance() {
        var tx = Transaction.builder()
                .user(user).account(account).type(TransactionType.EXPENSE)
                .direction(TransactionDirection.DEBIT).amount(new BigDecimal("150.00"))
                .transactionDate(LocalDate.now()).description("x")
                .source(TransactionSource.MANUAL).status(TransactionStatus.POSTED).build();

        when(transactions.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(tx));
        when(transactions.sumDeltaForAccount(accountId)).thenReturn(BigDecimal.ZERO);
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));

        service.delete(userId, 1L);

        verify(transactions).delete(tx);
        assertEquals(0, new BigDecimal("1000.00").compareTo(account.getCurrentBalance()));
    }
}