package com.financeflow.ledger.dto;

import com.financeflow.ledger.LedgerAccountClass;

import java.math.BigDecimal;

public record LedgerAccountSummaryResponse(
        Long id,
        Long financialAccountId,
        String code,
        String name,
        LedgerAccountClass accountClass,
        String currency,
        boolean active,
        BigDecimal bookedBalance,
        BigDecimal availableBalance,
        BigDecimal projectedBalance
) {}
