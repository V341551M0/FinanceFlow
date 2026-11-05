package com.financeflow.market;

import java.math.BigDecimal;

/**
 * Cotação de um ativo. price pode ser null quando o provedor não tem dados
 * — nesse caso, source="UNAVAILABLE" e asOf=null.
 */
public record Quote(
        BigDecimal price,
        BigDecimal changePct24h,
        String source,
        String asOf
) {
    public static Quote unavailable() {
        return new Quote(null, null, "UNAVAILABLE", null);
    }
}