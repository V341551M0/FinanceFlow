package com.financeflow.budget.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBudgetRequest(
        Long categoryId,
        @NotNull LocalDate referenceMonth,
        @NotNull @DecimalMin("0.01") BigDecimal limitAmount
) {}