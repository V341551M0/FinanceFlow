package com.financeflow.market;

import com.financeflow.investment.AssetType;
import com.financeflow.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketDataProvider provider;
    private final WatchlistService watchlist;

    public MarketController(MarketDataProvider provider, WatchlistService watchlist) {
        this.provider = provider;
        this.watchlist = watchlist;
    }

    @GetMapping("/quote")
    public Quote quote(@RequestParam String symbol,
                       @RequestParam AssetType type,
                       @RequestParam(required = false) String exchange) {
        return provider.getQuote(symbol, type, exchange);
    }

    @GetMapping("/candles")
    public List<Candle> candles(@RequestParam String symbol,
                                @RequestParam AssetType type,
                                @RequestParam(defaultValue = "1d") String interval,
                                @RequestParam(defaultValue = "100") int limit,
                                @RequestParam(required = false) String exchange) {
        return provider.getCandles(symbol, type, exchange, interval, Math.min(limit, 500));
    }

    @GetMapping("/watchlist")
    public List<WatchlistService.WatchlistEntry> watchlist(@AuthenticationPrincipal UserPrincipal p) {
        return watchlist.list(p.getId());
    }

    @PostMapping("/watchlist")
    public void add(@AuthenticationPrincipal UserPrincipal p,
                    @RequestParam String symbol,
                    @RequestParam AssetType type,
                    @RequestParam(required = false) String exchange) {
        watchlist.add(p.getId(), symbol, type, exchange);
    }

    @DeleteMapping("/watchlist/{id}")
    public void remove(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        watchlist.remove(p.getId(), id);
    }
}