package com.financeflow.investment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<WatchlistItem> findByUserIdAndAssetId(Long userId, Long assetId);
    boolean existsByUserIdAndAssetId(Long userId, Long assetId);
}