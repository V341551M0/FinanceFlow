package com.financeflow.account;

import com.financeflow.account.dto.CreateAccountRequest;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.ledger.DoubleEntryLedgerService;
import com.financeflow.ledger.LedgerAccount;
import com.financeflow.ledger.LedgerAccountClass;
import com.financeflow.user.User;
import com.financeflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock FinancialAccountRepository accounts;
    @Mock UserRepository users;
    @Mock DoubleEntryLedgerService ledger;
    @InjectMocks AccountService service;

    private User user;

    @BeforeEach
    void setup() {
        user = User.builder()
                .name("Test").email("t@t.com").passwordHash("x")
                .build();
        user.setId(1L);
    }

    @Test
    void create_sets_currentBalanceEqualToInitial_andPostsOpeningEntry() {
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(accounts.save(any(FinancialAccount.class))).thenAnswer(inv -> {
            FinancialAccount a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        var assetLedger = LedgerAccount.builder().code("ASSET:FIN_ACC:10").accountClass(LedgerAccountClass.ASSET).build();
        assetLedger.setId(100L);
        var equityLedger = LedgerAccount.builder().code("EQUITY:OPENING_BALANCE:BRL").accountClass(LedgerAccountClass.EQUITY).build();
        equityLedger.setId(200L);

        when(ledger.ensureAccountForFinancialAccount(any())).thenReturn(assetLedger);
        when(ledger.ensureSystemAccount(any(), any(), any(), eq(LedgerAccountClass.EQUITY), eq("BRL")))
                .thenReturn(equityLedger);

        var req = new CreateAccountRequest(
                "Conta Corrente", "Itaú", AccountType.CHECKING,
                "BRL", new BigDecimal("1500.00"));

        var res = service.create(1L, req);

        assertEquals(0, new BigDecimal("1500.00").compareTo(res.initialBalance()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(res.currentBalance()));
        assertEquals("Conta Corrente", res.name());
        assertEquals(AccountType.CHECKING, res.type());
        assertTrue(res.active());
        verify(ledger).postEntry(any());
    }

    @Test
    void create_throwsWhenUserNotFound() {
        when(users.findById(99L)).thenReturn(Optional.empty());
        var req = new CreateAccountRequest(
                "X", "Y", AccountType.CHECKING, "BRL", BigDecimal.ZERO);
        assertThrows(NotFoundException.class, () -> service.create(99L, req));
    }

    @Test
    void get_isolatesByUser() {
        when(accounts.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.get(2L, 5L));
    }
}
