package com.financeflow.dashboard;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DateRangeTest {

    @Test
    void previousPeriod_keepsSameLength_forCalendarMonth() {
        // Out/2026 é mês calendário completo → setembro inteiro
        var r = new DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        var prev = r.previousPeriod();
        assertEquals(LocalDate.of(2026, 9, 1), prev.start());
        assertEquals(LocalDate.of(2026, 9, 30), prev.end());
    }

    @Test
    void previousPeriod_keepsSameLength_forNonCalendarRange() {
        // 15/10 a 19/10 (5 dias) → período anterior = 10/10 a 14/10 (5 dias)
        var r = new DateRange(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 19));
        var prev = r.previousPeriod();
        assertEquals(LocalDate.of(2026, 10, 10), prev.start());
        assertEquals(LocalDate.of(2026, 10, 14), prev.end());
    }

    @Test
    void thisMonth_resolvesCorrectly() {
        var today = LocalDate.of(2026, 11, 15);
        var r = PeriodPreset.THIS_MONTH.resolve(today);
        assertEquals(LocalDate.of(2026, 11, 1), r.start());
        assertEquals(today, r.end());
    }

    @Test
    void lastMonth_resolvesCorrectly() {
        var today = LocalDate.of(2026, 3, 5);
        var r = PeriodPreset.LAST_MONTH.resolve(today);
        assertEquals(LocalDate.of(2026, 2, 1), r.start());
        assertEquals(LocalDate.of(2026, 2, 28), r.end());
    }

    @Test
    void custom_throws() {
        assertThrows(IllegalStateException.class,
                () -> PeriodPreset.CUSTOM.resolve(LocalDate.now()));
    }

    @Test
    void invalidRange_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new DateRange(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 1, 1)));
    }
}
