package com.financeflow.dashboard.dto;

import java.util.List;

public record DashboardResponse(
        DashboardSummary summary,
        List<TimeSeriesPoint> evolution,
        List<CategoryBreakdown> expenseByCategory,
        List<CategoryBreakdown> incomeByCategory
) {}