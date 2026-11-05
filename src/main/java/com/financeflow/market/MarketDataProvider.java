package com.financeflow.market;

import com.financeflow.investment.AssetType;

import java.util.List;

/**
 * Abstração de provedor de cotações. Implementações:
 *  - BrapiMarketDataProvider (B3: STOCK, ETF, FII, BDR)
 *  - CoinGeckoMarketDataProvider (CRYPTO)
 */
public interface MarketDataProvider {

    boolean supports(AssetType type);

    Quote getQuote(String symbol, AssetType type, String exchange);

    /**
     * Série OHLCV para gráficos. Retorna lista vazia se indisponível.
     */
    List<Candle> getCandles(String symbol, AssetType type, String exchange, String interval, int limit);

    String name();
}