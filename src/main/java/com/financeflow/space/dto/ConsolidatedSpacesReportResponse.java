package com.financeflow.space.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ConsolidatedSpacesReportResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalOperationalIncome,
        BigDecimal totalOperationalExpense,
        BigDecimal totalOperatingResult,
        BigDecimal consolidatedProfitMarginPercent,
        BigDecimal netInterSpaceTransfers,
        List<SpaceReportResponse> spaces
) {}
