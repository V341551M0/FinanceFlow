package com.financeflow.space;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialSpaceRepository extends JpaRepository<FinancialSpace, Long> {
    List<FinancialSpace> findAllByUserIdAndActiveTrueOrderByDefaultSpaceDescNameAsc(Long userId);
    Optional<FinancialSpace> findByIdAndUserId(Long id, Long userId);
    Optional<FinancialSpace> findByUserIdAndDefaultSpaceTrue(Long userId);
    boolean existsByUserIdAndSlug(Long userId, String slug);
}
