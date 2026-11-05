package com.financeflow.investment.dto;

import com.financeflow.investment.AssetType;

import java.math.BigDecimal;

public record PositionResponse(
        Long investmentId,
        Long assetId,
        String symbol,
        String name,
        AssetType type,
        String exchange,
        String currency,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal investedValue,
        BigDecimal marketValue,
        BigDecimal profitLoss,
        BigDecimal profitLossPct,
        String priceSource,
        String priceUpdatedAt
) {}