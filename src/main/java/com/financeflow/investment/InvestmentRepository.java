package com.financeflow.investment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findAllByUserIdOrderByAssetSymbolAsc(Long userId);
    Optional<Investment> findByUserIdAndAssetId(Long userId, Long assetId);
    Optional<Investment> findByIdAndUserId(Long id, Long userId);
}