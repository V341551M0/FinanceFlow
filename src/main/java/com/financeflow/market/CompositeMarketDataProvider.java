package com.financeflow.market;

import com.financeflow.investment.AssetType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CompositeMarketDataProvider implements MarketDataProvider {

    private final List<MarketDataProvider> providers;

    public CompositeMarketDataProvider(List<MarketDataProvider> providers) {
        this.providers = providers;
    }

    @Override public boolean supports(AssetType type) {
        return providers.stream().anyMatch(p -> p.supports(type));
    }

    @Override
    public Quote getQuote(String symbol, AssetType type, String exchange) {
        return providers.stream()
                .filter(p -> p.supports(type))
                .findFirst()
                .map(p -> p.getQuote(symbol, type, exchange))
                .orElse(Quote.unavailable());
    }

    @Override
    public List<Candle> getCandles(String symbol, AssetType type, String exchange, String interval, int limit) {
        return providers.stream()
                .filter(p -> p.supports(type))
                .findFirst()
                .map(p -> p.getCandles(symbol, type, exchange, interval, limit))
                .orElse(List.of());
    }

    @Override public String name() { return "composite"; }
}