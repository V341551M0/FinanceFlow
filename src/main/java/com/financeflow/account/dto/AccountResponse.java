package com.financeflow.account.dto;

import com.financeflow.account.AccountType;
import com.financeflow.account.FinancialAccount;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String name,
        String institution,
        AccountType type,
        String currency,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        Boolean active,
        Instant createdAt
) {
    public static AccountResponse from(FinancialAccount a) {
        return new AccountResponse(
                a.getId(), a.getName(), a.getInstitution(), a.getType(),
                a.getCurrency(), a.getInitialBalance(), a.getCurrentBalance(),
                a.getActive(), a.getCreatedAt());
    }
}