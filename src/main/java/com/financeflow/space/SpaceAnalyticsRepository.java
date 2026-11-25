package com.financeflow.space;

import com.financeflow.transaction.Transaction;
import com.financeflow.transaction.TransactionStatus;
import com.financeflow.transaction.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SpaceAnalyticsRepository extends JpaRepository<Transaction, Long> {

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.space.id = :spaceId
          AND t.type = :type
          AND t.status = :status
          AND t.transactionDate BETWEEN :from AND :to
    """)
    BigDecimal sumBySpaceAndType(
            @Param("userId") Long userId,
            @Param("spaceId") Long spaceId,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.space.id = :spaceId
          AND t.status <> com.financeflow.transaction.TransactionStatus.IGNORED
          AND t.transactionDate BETWEEN :from AND :to
    """)
    long countTransactionsInPeriod(
            @Param("userId") Long userId,
            @Param("spaceId") Long spaceId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT COUNT(a) FROM FinancialAccount a WHERE a.user.id = :userId AND a.space.id = :spaceId AND a.active = TRUE")
    long countActiveAccounts(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    @Query("SELECT COUNT(g) FROM FinancialGoal g WHERE g.user.id = :userId AND g.space.id = :spaceId AND g.completed = FALSE")
    long countActiveGoals(@Param("userId") Long userId, @Param("spaceId") Long spaceId);
}
