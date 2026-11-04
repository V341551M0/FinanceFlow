package com.financeflow.budget.dto;

import java.math.BigDecimal;

public record BudgetResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String color,
        BigDecimal limitAmount,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentageUsed,
        boolean exceeded
) {}