package com.financeflow.statement;

import com.financeflow.transaction.TransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedTransaction(
        LocalDate date,
        String description,
        BigDecimal amount,
        TransactionDirection direction,
        String externalId,
        String error    // null = válido
) {
    public boolean isValid() { return error == null; }
}