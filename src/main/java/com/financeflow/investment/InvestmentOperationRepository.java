package com.financeflow.investment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentOperationRepository extends JpaRepository<InvestmentOperation, Long> {
    List<InvestmentOperation> findAllByInvestmentIdOrderByOperationDateDesc(Long investmentId);
    List<InvestmentOperation> findAllByInvestmentUserIdOrderByOperationDateDesc(Long userId);
}