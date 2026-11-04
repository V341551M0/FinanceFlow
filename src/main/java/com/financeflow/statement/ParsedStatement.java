package com.financeflow.statement;

import java.math.BigDecimal;
import java.util.List;

public record ParsedStatement(
        StatementFormat format,
        List<ParsedTransaction> transactions,
        BigDecimal declaredFinalBalance,   // null se não informado
        List<String> warnings
) {}