package com.financeflow.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findAllByUserIdAndReferenceMonth(Long userId, LocalDate referenceMonth);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
}