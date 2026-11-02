package com.financeflow.statement.dto;

import java.math.BigDecimal;

public record ImportResultResponse(
        int imported,
        int skippedDuplicates,
        int skippedInvalid,
        BigDecimal newAccountBalance,
        BigDecimal declaredFinalBalance,
        boolean reconciled
) {}