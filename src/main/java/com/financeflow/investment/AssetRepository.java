package com.financeflow.investment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findBySymbolIgnoreCaseAndExchange(String symbol, String exchange);
    Optional<Asset> findBySymbolIgnoreCase(String symbol);
    List<Asset> findAllBySymbolInIgnoreCase(List<String> symbols);
}