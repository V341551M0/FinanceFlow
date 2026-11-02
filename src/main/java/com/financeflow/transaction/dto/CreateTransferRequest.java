package com.financeflow.transaction.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransferRequest(
        @NotNull Long sourceAccountId,
        @NotNull Long destinationAccountId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate transactionDate,
        @NotBlank @Size(max = 255) String description
) {}