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
import java.util.Set;

@Component
public class BrapiMarketDataProvider implements MarketDataProvider {

    private static final Set<AssetType> SUPPORTED =
            Set.of(AssetType.STOCK, AssetType.ETF, AssetType.FII, AssetType.BDR);

    private final RestClient http;
    private final String token;

    public BrapiMarketDataProvider(@Value("${financeflow.market.brapi.token:}") String token) {
        this.token = token;
        this.http = RestClient.builder()
                .baseUrl("https://brapi.dev/api")
                .build();
    }

    @Override public boolean supports(AssetType type) { return SUPPORTED.contains(type); }

    @Override
    @Cacheable(value = "quotes", key = "'brapi:' + #symbol")
    public Quote getQuote(String symbol, AssetType type, String exchange) {
        try {
            var spec = http.get().uri(uri -> {
                var b = uri.path("/quote/{symbol}").build(symbol);
                return token.isBlank() ? b : uri.queryParam("token", token).path("/quote/{symbol}").build(symbol);
            });
            JsonNode root = spec.retrieve().body(JsonNode.class);
            if (root == null || !root.has("results") || root.get("results").isEmpty()) {
                return Quote.unavailable();
            }
            var r = root.get("results").get(0);
            var price = r.hasNonNull("regularMarketPrice")
                    ? new BigDecimal(r.get("regularMarketPrice").asText()) : null;
            var change = r.hasNonNull("regularMarketChangePercent")
                    ? new BigDecimal(r.get("regularMarketChangePercent").asText()) : null;
            var time = r.hasNonNull("regularMarketTime")
                    ? r.get("regularMarketTime").asText() : Instant.now().toString();
            return new Quote(price, change, "brapi.dev", time);
        } catch (Exception e) {
            return Quote.unavailable();
        }
    }

    @Override
    public List<Candle> getCandles(String symbol, AssetType type, String exchange, String interval, int limit) {
        try {
            var spec = http.get().uri(uri -> {
                var b = uri.path("/quote/{symbol}")
                        .queryParam("range", mapRange(interval))
                        .queryParam("interval", mapInterval(interval))
                        .build(symbol);
                return token.isBlank() ? b : uri.queryParam("token", token)
                        .path("/quote/{symbol}")
                        .queryParam("range", mapRange(interval))
                        .queryParam("interval", mapInterval(interval))
                        .build(symbol);
            });
            JsonNode root = spec.retrieve().body(JsonNode.class);
            if (root == null || !root.has("results") || root.get("results").isEmpty()) return List.of();
            var hist = root.get("results").get(0).get("historicalDataPrice");
            if (hist == null || !hist.isArray()) return List.of();

            List<Candle> out = new ArrayList<>();
            for (JsonNode c : hist) {
                out.add(new Candle(
                        Instant.ofEpochSecond(c.get("date").asLong()),
                        new BigDecimal(c.get("open").asText()),
                        new BigDecimal(c.get("high").asText()),
                        new BigDecimal(c.get("low").asText()),
                        new BigDecimal(c.get("close").asText()),
                        c.hasNonNull("volume") ? new BigDecimal(c.get("volume").asText()) : BigDecimal.ZERO));
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override public String name() { return "brapi.dev"; }

    private String mapRange(String interval) {
        return switch (interval) {
            case "1m", "5m", "15m", "30m", "1h", "4h" -> "5d";
            case "1d" -> "3mo";
            case "1wk" -> "1y";
            case "1mo" -> "5y";
            default -> "1mo";
        };
    }

    private String mapInterval(String interval) {
        return switch (interval) {
            case "1m", "5m", "15m", "30m", "1h", "4h" -> "1d";
            case "1d" -> "1d";
            case "1wk" -> "1wk";
            case "1mo" -> "1mo";
            default -> "1d";
        };
    }
}