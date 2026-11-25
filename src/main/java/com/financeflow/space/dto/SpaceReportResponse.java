package com.financeflow.space.dto;

import com.financeflow.space.SpaceType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SpaceReportResponse(
        Long spaceId,
        String spaceName,
        SpaceType spaceType,
        String currency,
        LocalDate from,
        LocalDate to,
        BigDecimal operationalIncome,
        BigDecimal operationalExpense,
        BigDecimal operatingResult,
        BigDecimal profitMarginPercent,
        BigDecimal transfersIn,
        BigDecimal transfersOut,
        BigDecimal netSpaceCashFlow,
        long transactionCount,
        long linkedAccountsCount,
        long activeGoalsCount
) {}
