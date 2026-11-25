package com.financeflow.notification;

import com.financeflow.budget.BudgetRepository;
import com.financeflow.creditcard.CreditCardInvoiceRepository;
import com.financeflow.creditcard.InvoiceStatus;
import com.financeflow.goal.FinancialGoalRepository;
import com.financeflow.transaction.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    @PersistenceContext private EntityManager em;

    private final BudgetRepository budgets;
    private final FinancialGoalRepository goals;
    private final CreditCardInvoiceRepository invoices;
    private final NotificationService notifications;

    public NotificationScheduler(BudgetRepository budgets,
                                 FinancialGoalRepository goals,
                                 CreditCardInvoiceRepository invoices,
                                 NotificationService notifications) {
        this.budgets = budgets;
        this.goals = goals;
        this.invoices = invoices;
        this.notifications = notifications;
    }

    @Scheduled(cron = "0 0 8 * * *")  // 08:00 todos os dias
    @Transactional
    public void dailyCheck() {
        log.info("NotificationScheduler: iniciando verificação diária");
        checkBudgets();
        checkInvoices();
        checkGoals();
        log.info("NotificationScheduler: verificação concluída");
    }

    private void checkBudgets() {
        var month = LocalDate.now().withDayOfMonth(1);
        var allBudgets = budgets.findAllByReferenceMonth(month);
        var monthKey = month.toString();

        for (var b : allBudgets) {
            var spent = sumExpense(b.getUser().getId(), b.getCategory() != null
                    ? b.getCategory().getId() : null, month);
            if (b.getLimitAmount().signum() == 0) continue;
            var pct = spent.multiply(BigDecimal.valueOf(100))
                    .divide(b.getLimitAmount(), 0, RoundingMode.HALF_UP)
                    .intValue();

            var cat = b.getCategory() != null ? b.getCategory().getName() : "Geral";

            if (pct >= 100) {
                notifications.createIfAbsent(b.getUser(), NotificationType.BUDGET_EXCEEDED,
                        "Orçamento excedido",
                        "Você excedeu o orçamento de " + cat + " (" + pct + "% usado)",
                        "budget-exceeded:" + b.getId() + ":" + monthKey);
            } else if (pct >= 80) {
                notifications.createIfAbsent(b.getUser(), NotificationType.BUDGET_WARNING,
                        "Orçamento próximo do limite",
                        "Você usou " + pct + "% do orçamento de " + cat,
                        "budget-warning:" + b.getId() + ":" + monthKey);
            }
        }
    }

    private void checkInvoices() {
        var today = LocalDate.now();
        var in7 = today.plusDays(7);
        var due = invoices.findAllByStatusAndDueDateBetween(InvoiceStatus.OPEN, today, in7);
        for (var inv : due) {
            var card = inv.getCard();
            notifications.createIfAbsent(card.getUser(), NotificationType.INVOICE_DUE,
                    "Fatura próxima do vencimento",
                    "Fatura " + card.getName() + " vence em " + inv.getDueDate(),
                    "invoice-due:" + inv.getId());
        }
    }

    private void checkGoals() {
        for (var g : goals.findAllByCompletedTrue()) {
            notifications.createIfAbsent(g.getUser(), NotificationType.GOAL_REACHED,
                    "Meta atingida! 🎉",
                    "Parabéns! Você concluiu a meta " + g.getName(),
                    "goal-reached:" + g.getId());
        }
    }

    private BigDecimal sumExpense(Long userId, Long categoryId, LocalDate month) {
        var from = month;
        var to = month.withDayOfMonth(month.lengthOfMonth());
        var jpql = categoryId == null
                ? "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
                  "WHERE t.user.id = :uid AND t.type = com.financeflow.transaction.TransactionType.EXPENSE " +
                  "AND t.transactionDate BETWEEN :from AND :to"
                : "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
                  "WHERE t.user.id = :uid AND t.category.id = :cid " +
                  "AND t.type = com.financeflow.transaction.TransactionType.EXPENSE " +
                  "AND t.transactionDate BETWEEN :from AND :to";
        var q = em.createQuery(jpql, BigDecimal.class)
                .setParameter("uid", userId).setParameter("from", from).setParameter("to", to);
        if (categoryId != null) q.setParameter("cid", categoryId);
        return q.getSingleResult();
    }
}
