package com.financeflow.dashboard;

import com.financeflow.dashboard.dto.CategoryBreakdown;
import com.financeflow.dashboard.dto.TimeSeriesPoint;
import com.financeflow.transaction.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class DashboardQueries {

    @PersistenceContext
    private EntityManager em;

    public BigDecimal sumByType(Long userId, LocalDate from, LocalDate to,
                                String type, String direction) {
        var q = em.createQuery("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user.id = :uid
              AND t.transactionDate BETWEEN :from AND :to
              AND t.status <> com.financeflow.transaction.TransactionStatus.IGNORED
              AND (:type IS NULL OR t.type = :type)
              AND t.direction = :direction
            """, BigDecimal.class);
        q.setParameter("uid", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("type", type == null ? null : com.financeflow.transaction.TransactionType.valueOf(type))
                .setParameter("direction", com.financeflow.transaction.TransactionDirection.valueOf(direction));
        return q.getSingleResult();
    }

    public BigDecimal sumAllPatrimony(Long userId) {
        return em.createQuery("""
            SELECT COALESCE(SUM(a.currentBalance), 0)
            FROM FinancialAccount a
            WHERE a.user.id = :uid AND a.active = TRUE
            """, BigDecimal.class)
                .setParameter("uid", userId)
                .getSingleResult();
    }

    public List<TimeSeriesPoint> dailySeries(Long userId, LocalDate from, LocalDate to, BigDecimal baseBalance) {
        var rows = em.createQuery("""
            SELECT t.transactionDate,
                   SUM(CASE WHEN t.type = com.financeflow.transaction.TransactionType.INCOME
                                 AND t.direction = com.financeflow.transaction.TransactionDirection.CREDIT
                            THEN t.amount ELSE 0 END),
                   SUM(CASE WHEN t.type = com.financeflow.transaction.TransactionType.EXPENSE
                                 AND t.direction = com.financeflow.transaction.TransactionDirection.DEBIT
                            THEN t.amount ELSE 0 END),
                   SUM(CASE WHEN t.direction = com.financeflow.transaction.TransactionDirection.CREDIT
                            THEN t.amount ELSE -t.amount END)
            FROM Transaction t
            WHERE t.user.id = :uid
              AND t.transactionDate BETWEEN :from AND :to
              AND t.status <> com.financeflow.transaction.TransactionStatus.IGNORED
            GROUP BY t.transactionDate
            ORDER BY t.transactionDate
            """, Object[].class)
                .setParameter("uid", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        var result = new ArrayList<TimeSeriesPoint>();
        var running = baseBalance;

        // Preenche dias sem movimento com valor zerado (saldo continua)
        var byDate = new java.util.HashMap<LocalDate, Object[]>();
        for (var r : rows) byDate.put((LocalDate) r[0], r);

        var cursor = from;
        while (!cursor.isAfter(to)) {
            var r = byDate.get(cursor);
            BigDecimal inc = BigDecimal.ZERO, exp = BigDecimal.ZERO, delta = BigDecimal.ZERO;
            if (r != null) {
                inc = (BigDecimal) r[1];
                exp = (BigDecimal) r[2];
                delta = (BigDecimal) r[3];
            }
            running = running.add(delta);
            result.add(new TimeSeriesPoint(cursor, inc, exp, running));
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    public List<CategoryBreakdown> breakdownByCategory(Long userId, LocalDate from, LocalDate to,
                                                       com.financeflow.transaction.TransactionType type) {
        var rows = em.createQuery("""
            SELECT COALESCE(c.id, 0), COALESCE(c.name, 'Sem categoria'), c.color, SUM(t.amount)
            FROM Transaction t
            LEFT JOIN t.category c
            WHERE t.user.id = :uid
              AND t.transactionDate BETWEEN :from AND :to
              AND t.type = :type
              AND t.status <> com.financeflow.transaction.TransactionStatus.IGNORED
            GROUP BY c.id, c.name, c.color
            ORDER BY SUM(t.amount) DESC
            """, Object[].class)
                .setParameter("uid", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("type", type)
                .getResultList();

        BigDecimal total = rows.stream()
                .map(r -> (BigDecimal) r[3])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream().map(r -> {
            var amount = (BigDecimal) r[3];
            var pct = total.signum() == 0 ? BigDecimal.ZERO :
                    amount.multiply(BigDecimal.valueOf(100))
                            .divide(total, 2, java.math.RoundingMode.HALF_UP);
            return new CategoryBreakdown(
                    ((Number) r[0]).longValue(),
                    (String) r[1],
                    (String) r[2],
                    amount,
                    pct);
        }).toList();
    }
}