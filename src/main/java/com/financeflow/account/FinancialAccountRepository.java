package com.financeflow.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, Long> {
    List<FinancialAccount> findAllByUserIdAndActiveTrueOrderByNameAsc(Long userId);
    Optional<FinancialAccount> findByIdAndUserId(Long id, Long userId);
}