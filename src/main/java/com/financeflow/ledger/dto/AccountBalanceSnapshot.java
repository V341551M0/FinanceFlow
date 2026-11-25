package com.financeflow.ledger.dto;

import java.math.BigDecimal;

public record AccountBalanceSnapshot(
        Long ledgerAccountId,
        String code,
        String currency,
        BigDecimal bookedBalance,
        BigDecimal availableBalance,
        BigDecimal projectedBalance
) {}
