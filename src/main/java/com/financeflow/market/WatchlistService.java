package com.financeflow.market;

import com.financeflow.common.exception.BusinessException;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.investment.*;
import com.financeflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlist;
    private final AssetRepository assets;
    private final UserRepository users;
    private final MarketDataProvider marketData;

    public WatchlistService(WatchlistRepository watchlist,
                            AssetRepository assets,
                            UserRepository users,
                            MarketDataProvider marketData) {
        this.watchlist = watchlist;
        this.assets = assets;
        this.users = users;
        this.marketData = marketData;
    }

    @Transactional
    public void add(Long userId, String symbol, AssetType type, String exchange) {
        if (watchlist.findAllByUserIdOrderByCreatedAtDesc(userId).size() >= 50) {
            throw new BusinessException("Watchlist limitada a 50 ativos.");
        }
        var asset = assets.findBySymbolIgnoreCaseAndExchange(symbol, exchange)
                .orElseGet(() -> assets.save(Asset.builder()
                        .symbol(symbol.toUpperCase()).name(symbol).type(type)
                        .exchange(exchange).currency("BRL").build()));
        if (watchlist.existsByUserIdAndAssetId(userId, asset.getId())) {
            throw new BusinessException("Ativo já está na watchlist.");
        }
        var user = users.findById(userId).orElseThrow();
        watchlist.save(WatchlistItem.builder().user(user).asset(asset).build());
    }

    @Transactional(readOnly = true)
    public List<WatchlistEntry> list(Long userId) {
        return watchlist.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(item -> {
            var a = item.getAsset();
            var q = marketData.getQuote(a.getSymbol(), a.getType(), a.getExchange());
            return new WatchlistEntry(item.getId(), a.getSymbol(), a.getName(),
                    a.getType(), a.getExchange(), q.price(), q.changePct24h(),
                    q.source(), q.asOf());
        }).toList();
    }

    @Transactional
    public void remove(Long userId, Long itemId) {
        var item = watchlist.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Item não encontrado"));
        watchlist.delete(item);
    }

    public record WatchlistEntry(Long id, String symbol, String name, AssetType type,
                                 String exchange, java.math.BigDecimal price,
                                 java.math.BigDecimal changePct24h, String source, String asOf) {}
}