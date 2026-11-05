package com.financeflow.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface TransactionRepository extends
        JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    /** Δ do saldo da conta: +amount p/ CREDIT, -amount p/ DEBIT. */
    @Query("""
        SELECT COALESCE(SUM(
            CASE WHEN t.direction = com.financeflow.transaction.TransactionDirection.CREDIT
                 THEN t.amount ELSE -t.amount END
        ), 0)
        FROM Transaction t
        WHERE t.account.id = :accountId
          AND t.status <> com.financeflow.transaction.TransactionStatus.IGNORED
    """)
    BigDecimal sumDeltaForAccount(@Param("accountId") Long accountId);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.id = :userId
          AND t.account.id = :accountId
          AND t.externalId = :externalId
    """)
    Optional<Transaction> findByAccountAndExternalId(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("externalId") String externalId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT t FROM Transaction t
        LEFT JOIN FETCH t.category
        LEFT JOIN FETCH t.account
        WHERE t.user.id = :userId
          AND t.transactionDate BETWEEN :from AND :to
          AND t.status <> com.financeflow.transaction.TransactionStatus.IGNORED
        ORDER BY t.transactionDate DESC, t.id DESC
        """)
    java.util.List<Transaction> findForReport(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("from") java.time.LocalDate from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDate to);
}