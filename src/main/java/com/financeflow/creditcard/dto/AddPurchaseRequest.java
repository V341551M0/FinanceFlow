package com.financeflow.creditcard.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddPurchaseRequest(
        Long categoryId,
        @NotBlank @Size(max = 255) String description,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate transactionDate,
        @NotNull @Min(1) @Max(48) Integer installments
) {}