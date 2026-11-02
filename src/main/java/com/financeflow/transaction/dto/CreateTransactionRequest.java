package com.financeflow.transaction.dto;

import com.financeflow.transaction.TransactionDirection;
import com.financeflow.transaction.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
        @NotNull Long accountId,
        Long categoryId,
        @NotNull TransactionType type,
        @NotNull TransactionDirection direction,
        @NotNull @DecimalMin("0.00") BigDecimal amount,
        @NotNull LocalDate transactionDate,
        @NotBlank @Size(max = 255) String description,
        @Size(max = 500) String notes
) {}