package com.financeflow.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardSummary(
        BigDecimal totalPatrimony,
        BigDecimal grossIncome,
        BigDecimal refunds,
        BigDecimal netIncome,
        BigDecimal grossExpense,
        BigDecimal netExpense,
        BigDecimal netResult,
        BigDecimal savingsRate,
        BigDecimal previousNetResult,
        BigDecimal netResultChangePct,
        BigDecimal previousNetExpense,
        BigDecimal netExpenseChangePct,
        BigDecimal avgDailyExpense,
        LocalDate startDate,
        LocalDate endDate
) {}