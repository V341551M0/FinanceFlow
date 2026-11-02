package com.financeflow.statement.dto;

import com.financeflow.transaction.TransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PreviewRowResponse(
        LocalDate date,
        String description,
        BigDecimal amount,
        TransactionDirection direction,
        boolean valid,
        boolean duplicate,
        String error
) {}