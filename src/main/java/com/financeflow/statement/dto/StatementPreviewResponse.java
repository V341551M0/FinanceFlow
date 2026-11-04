package com.financeflow.statement.dto;

import java.math.BigDecimal;
import java.util.List;

public record StatementPreviewResponse(
        String format,
        int totalRows,
        int validRows,
        int invalidRows,
        int duplicateRows,
        BigDecimal declaredFinalBalance,
        BigDecimal projectedBalance,
        boolean reconciled,
        List<String> warnings,
        List<PreviewRowResponse> rows
) {}