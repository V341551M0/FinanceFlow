package com.financeflow.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {
    Optional<LedgerAccount> findByIdAndUserId(Long id, Long userId);
    Optional<LedgerAccount> findByUserIdAndCode(Long userId, String code);
    Optional<LedgerAccount> findByFinancialAccountId(Long financialAccountId);
    List<LedgerAccount> findByUserIdAndActiveTrueOrderByCodeAsc(Long userId);
}
