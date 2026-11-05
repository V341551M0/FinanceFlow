package com.financeflow.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, Long> {
    List<FinancialGoal> findAllByUserIdOrderByCompletedAscTargetDateAsc(Long userId);
    Optional<FinancialGoal> findByIdAndUserId(Long id, Long userId);
}