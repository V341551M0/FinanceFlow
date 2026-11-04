package com.financeflow.dashboard;

import com.financeflow.dashboard.dto.*;
import com.financeflow.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class DashboardService {

    private final DashboardQueries queries;

    public DashboardService(DashboardQueries queries) { this.queries = queries; }

    @Transactional(readOnly = true)
    public DashboardResponse build(Long userId, DateRange range) {
        var from = range.start();
        var to = range.end();
        var prev = range.previousPeriod();

        // --- Período atual ---
        var grossIncome  = queries.sumByType(userId, from, to, "INCOME", "CREDIT");
        var refunds      = queries.sumByType(userId, from, to, "REFUND", "CREDIT");
        var grossExpense = queries.sumByType(userId, from, to, "EXPENSE", "DEBIT");

        var netIncome   = grossIncome.subtract(refunds);
        var netExpense  = grossExpense.subtract(refunds);
        var netResult   = netIncome.subtract(netExpense);

        var patrimony = queries.sumAllPatrimony(userId);

        // --- Período anterior ---
        var prevGrossIncome  = queries.sumByType(userId, prev.start(), prev.end(), "INCOME", "CREDIT");
        var prevRefunds      = queries.sumByType(userId, prev.start(), prev.end(), "REFUND", "CREDIT");
        var prevGrossExpense = queries.sumByType(userId, prev.start(), prev.end(), "EXPENSE", "DEBIT");
        var prevNetIncome   = prevGrossIncome.subtract(prevRefunds);
        var prevNetExpense  = prevGrossExpense.subtract(prevRefunds);
        var prevNetResult   = prevNetIncome.subtract(prevNetExpense);

        var netResultChangePct  = pctChange(netResult, prevNetResult);
        var netExpenseChangePct = pctChange(netExpense, prevNetExpense);

        var days = ChronoUnit.DAYS.between(from, to) + 1;
        var avgDailyExpense = days == 0 ? BigDecimal.ZERO :
                netExpense.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);

        var savingsRate = grossIncome.signum() == 0 ? BigDecimal.ZERO :
                netResult.multiply(BigDecimal.valueOf(100))
                        .divide(grossIncome, 2, RoundingMode.HALF_UP);

        var summary = new DashboardSummary(
                patrimony, grossIncome, refunds, netIncome,
                grossExpense, netExpense, netResult, savingsRate,
                prevNetResult, netResultChangePct,
                prevNetExpense, netExpenseChangePct,
                avgDailyExpense, from, to);

        // Base do saldo: patrimônio atual − Σ(CREDIT-DEBIT) no período
        var series = queries.dailySeries(userId, from, to, baseBalanceAtStart(userId, from));

        var expenseByCategory = queries.breakdownByCategory(userId, from, to, TransactionType.EXPENSE);
        var incomeByCategory  = queries.breakdownByCategory(userId, from, to, TransactionType.INCOME);

        return new DashboardResponse(summary, series, expenseByCategory, incomeByCategory);
    }

    private BigDecimal baseBalanceAtStart(Long userId, LocalDate from) {
        var patrimony = queries.sumAllPatrimony(userId);
        // Saldo no início do período = patrimônio atual − Σ(CREDIT-DEBIT) desde from até hoje
        var delta = queries.sumByType(userId, from, LocalDate.now(), null, "CREDIT")
                .subtract(queries.sumByType(userId, from, LocalDate.now(), null, "DEBIT"));
        return patrimony.subtract(delta);
    }

    private BigDecimal pctChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) return BigDecimal.ZERO;
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 2, RoundingMode.HALF_UP);
    }
}