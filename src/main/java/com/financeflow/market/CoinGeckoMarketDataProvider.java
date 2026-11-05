package com.financeflow.market;

import com.financeflow.investment.AssetType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class CoinGeckoMarketDataProvider implements MarketDataProvider {

    private final RestClient http;
    private final String apiKey;

    public CoinGeckoMarketDataProvider(@Value("${financeflow.market.coingecko.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.http = RestClient.builder()
                .baseUrl("https://api.coingecko.com/api/v3")
                .defaultHeader("x-cg-demo-api-key", apiKey.isBlank() ? "" : apiKey)
                .build();
    }

    @Override public boolean supports(AssetType type) { return type == AssetType.CRYPTO; }

    @Override
    @Cacheable(value = "quotes", key = "'cg:' + #symbol.toLowerCase()")
    public Quote getQuote(String symbol, AssetType type, String exchange) {
        try {
            var id = mapToId(symbol);
            JsonNode root = http.get()
                    .uri("/simple/price?ids={id}&vs_currencies=brl&include_24hr_change=true", id)
                    .retrieve().body(JsonNode.class);
            if (root == null || !root.has(id)) return Quote.unavailable();
            var node = root.get(id);
            var price = node.hasNonNull("brl") ? new BigDecimal(node.get("brl").asText()) : null;
            var change = node.hasNonNull("brl_24h_change")
                    ? new BigDecimal(node.get("brl_24h_change").asText()) : null;
            return new Quote(price, change, "coingecko", Instant.now().toString());
        } catch (Exception e) {
            return Quote.unavailable();
        }
    }

    @Override
    public List<Candle> getCandles(String symbol, AssetType type, String exchange, String interval, int limit) {
        try {
            var id = mapToId(symbol);
            var days = switch (interval) {
                case "1m", "5m", "15m", "30m", "1h", "4h" -> "1";
                case "1d" -> "30";
                case "1wk" -> "90";
                case "1mo" -> "365";
                default -> "30";
            };
            JsonNode root = http.get()
                    .uri("/coins/{id}/ohlc?vs_currency=brl&days={days}", id, days)
                    .retrieve().body(JsonNode.class);
            if (root == null || !root.isArray()) return List.of();
            List<Candle> out = new ArrayList<>();
            for (JsonNode c : root) {
                out.add(new Candle(
                        Instant.ofEpochMilli(c.get(0).asLong()),
                        new BigDecimal(c.get(1).asText()),
                        new BigDecimal(c.get(2).asText()),
                        new BigDecimal(c.get(3).asText()),
                        new BigDecimal(c.get(4).asText()),
                        BigDecimal.ZERO));
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override public String name() { return "coingecko"; }

    /** Mapeia símbolo para id canônico do CoinGecko (ex.: BTC → bitcoin). */
    private String mapToId(String symbol) {
        return switch (symbol.toUpperCase()) {
            case "BTC" -> "bitcoin";
            case "ETH" -> "ethereum";
            case "SOL" -> "solana";
            case "ADA" -> "cardano";
            case "DOT" -> "polkadot";
            case "MATIC" -> "matic-network";
            case "LINK" -> "chainlink";
            case "XRP" -> "ripple";
            case "DOGE" -> "dogecoin";
            case "AVAX" -> "avalanche-2";
            default -> symbol.toLowerCase();
        };
    }
}