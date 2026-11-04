package com.financeflow.dashboard;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public enum PeriodPreset {
    THIS_WEEK, LAST_WEEK,
    THIS_MONTH, LAST_MONTH,
    LAST_3_MONTHS, LAST_6_MONTHS,
    THIS_YEAR, LAST_YEAR, LAST_12_MONTHS,
    CUSTOM;

    public DateRange resolve(LocalDate today) {
        return switch (this) {
            case THIS_WEEK -> new DateRange(
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    today);
            case LAST_WEEK -> {
                var start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
                yield new DateRange(start, start.plusDays(6));
            }
            case THIS_MONTH -> new DateRange(today.withDayOfMonth(1), today);
            case LAST_MONTH -> {
                var start = today.withDayOfMonth(1).minusMonths(1);
                yield new DateRange(start, start.with(TemporalAdjusters.lastDayOfMonth()));
            }
            case LAST_3_MONTHS -> new DateRange(today.minusMonths(3), today);
            case LAST_6_MONTHS -> new DateRange(today.minusMonths(6), today);
            case THIS_YEAR -> new DateRange(today.withDayOfYear(1), today);
            case LAST_YEAR -> {
                var y = today.getYear() - 1;
                yield new DateRange(LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31));
            }
            case LAST_12_MONTHS -> new DateRange(today.minusMonths(12), today);
            case CUSTOM -> throw new IllegalStateException("CUSTOM requer datas explícitas");
        };
    }
}