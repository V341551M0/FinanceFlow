package com.financeflow.budget;

import com.financeflow.budget.dto.BudgetResponse;
import com.financeflow.budget.dto.CreateBudgetRequest;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.transaction.TransactionCategory;
import com.financeflow.transaction.TransactionCategoryRepository;
import com.financeflow.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class BudgetService {

    @PersistenceContext
    private EntityManager em;

    private final BudgetRepository budgets;
    private final TransactionCategoryRepository categories;
    private final UserRepository users;

    public BudgetService(BudgetRepository budgets,
                         TransactionCategoryRepository categories,
                         UserRepository users) {
        this.budgets = budgets;
        this.categories = categories;
        this.users = users;
    }

    @Transactional
    public BudgetResponse create(Long userId, CreateBudgetRequest req) {
        var user = users.findById(userId).orElseThrow();
        TransactionCategory cat = null;
        if (req.categoryId() != null) {
            cat = categories.findByIdAndUserId(req.categoryId(), userId)
                    .orElseThrow(() -> new NotFoundException("Categoria não encontrada"));
        }
        var month = req.referenceMonth().withDayOfMonth(1);
        var b = Budget.builder()
                .user(user).category(cat).referenceMonth(month)
                .limitAmount(req.limitAmount())
                .build();
        budgets.save(b);
        return toResponse(b, spentFor(userId, cat, month));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId, LocalDate referenceMonth) {
        var month = referenceMonth.withDayOfMonth(1);
        return budgets.findAllByUserIdAndReferenceMonth(userId, month).stream()
                .map(b -> toResponse(b, spentFor(userId, b.getCategory(), month)))
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        var b = budgets.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Orçamento não encontrado"));
        budgets.delete(b);
    }

    private BigDecimal spentFor(Long userId, TransactionCategory cat, LocalDate month) {
        var from = month;
        var to = month.withDayOfMonth(month.lengthOfMonth());
        var jpql = cat == null
                ? """
                  SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
                  WHERE t.user.id = :uid AND t.type = com.financeflow.transaction.TransactionType.EXPENSE
                    AND t.direction = com.financeflow.transaction.TransactionDirection.DEBIT
                    AND t.transactionDate BETWEEN :from AND :to
                  """
                : """
                  SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
                  WHERE t.user.id = :uid AND t.category.id = :cid
                    AND t.type = com.financeflow.transaction.TransactionType.EXPENSE
                    AND t.direction = com.financeflow.transaction.TransactionDirection.DEBIT
                    AND t.transactionDate BETWEEN :from AND :to
                  """;
        var q = em.createQuery(jpql, BigDecimal.class)
                .setParameter("uid", userId)
                .setParameter("from", from)
                .setParameter("to", to);
        if (cat != null) q.setParameter("cid", cat.getId());
        return q.getSingleResult();
    }

    private BudgetResponse toResponse(Budget b, BigDecimal spent) {
        var remaining = b.getLimitAmount().subtract(spent);
        var pct = b.getLimitAmount().signum() == 0 ? BigDecimal.ZERO :
                spent.multiply(BigDecimal.valueOf(100))
                        .divide(b.getLimitAmount(), 2, RoundingMode.HALF_UP);
        return new BudgetResponse(
                b.getId(),
                b.getCategory() == null ? null : b.getCategory().getId(),
                b.getCategory() == null ? "Geral" : b.getCategory().getName(),
                b.getCategory() == null ? null : b.getCategory().getColor(),
                b.getLimitAmount(), spent, remaining, pct,
                spent.compareTo(b.getLimitAmount()) > 0);
    }
}