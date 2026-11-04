package com.financeflow.goal.dto;

import com.financeflow.goal.FinancialGoal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record GoalResponse(
        Long id, String name,
        BigDecimal targetAmount, BigDecimal currentAmount,
        BigDecimal percentage,
        LocalDate targetDate, Boolean completed
) {
    public static GoalResponse from(FinancialGoal g) {
        var pct = g.getTargetAmount().signum() == 0 ? BigDecimal.ZERO :
                g.getCurrentAmount().multiply(BigDecimal.valueOf(100))
                        .divide(g.getTargetAmount(), 2, RoundingMode.HALF_UP);
        return new GoalResponse(
                g.getId(), g.getName(), g.getTargetAmount(),
                g.getCurrentAmount(), pct, g.getTargetDate(), g.getCompleted());
    }
}