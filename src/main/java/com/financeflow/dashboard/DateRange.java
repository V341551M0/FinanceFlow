package com.financeflow.dashboard;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public record DateRange(LocalDate start, LocalDate end) {

    public DateRange {
        if (start == null || end == null)
            throw new IllegalArgumentException("start e end são obrigatórios");
        if (start.isAfter(end))
            throw new IllegalArgumentException("start deve ser anterior a end");
    }

    /**
     * Período anterior de referência.
     * Se este range equivale a um mês calendário inteiro (dia 1 → último dia),
     * retorna o mês calendário anterior (ex.: Out/2026 → Set/2026).
     * Caso contrário, mantém o mesmo número de dias, terminando no dia anterior ao start.
     */
    public DateRange previousPeriod() {
        if (isFullCalendarMonth()) {
            var prevMonth = start.minusMonths(1).withDayOfMonth(1);
            return new DateRange(
                    prevMonth,
                    prevMonth.with(TemporalAdjusters.lastDayOfMonth()));
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        var prevEnd = start.minusDays(1);
        var prevStart = prevEnd.minusDays(days - 1);
        return new DateRange(prevStart, prevEnd);
    }

    private boolean isFullCalendarMonth() {
        return start.getDayOfMonth() == 1
                && end.equals(start.with(TemporalAdjusters.lastDayOfMonth()));
    }
}