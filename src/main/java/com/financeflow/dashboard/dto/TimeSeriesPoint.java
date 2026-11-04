package com.financeflow.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeSeriesPoint(
        LocalDate date,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal balance
) {}