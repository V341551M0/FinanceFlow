package com.financeflow.dashboard.dto;

import java.math.BigDecimal;

public record CategoryBreakdown(
        Long categoryId,
        String categoryName,
        String color,
        BigDecimal amount,
        BigDecimal percentage
) {}