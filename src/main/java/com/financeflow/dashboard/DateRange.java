package com.financeflow.dashboard;

import java.time.LocalDate;

public record DateRange(LocalDate start, LocalDate end) {
    public DateRange {
        if (start == null || end == null)
            throw new IllegalArgumentException("start e end são obrigatórios");
        if (start.isAfter(end))
            throw new IllegalArgumentException("start deve ser anterior a end");
    }

    public DateRange previousPeriod() {
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        var prevEnd = start.minusDays(1);
        var prevStart = prevEnd.minusDays(days - 1);
        return new DateRange(prevStart, prevEnd);
    }
}