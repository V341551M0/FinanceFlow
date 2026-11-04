package com.financeflow.transaction;

import com.financeflow.account.FinancialAccount;
import com.financeflow.account.FinancialAccountRepository;
import com.financeflow.common.exception.BusinessException;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.transaction.dto.CreateTransactionRequest;
import com.financeflow.transaction.dto.CreateTransferRequest;
import com.financeflow.transaction.dto.TransactionResponse;
import com.financeflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final FinancialAccountRepository accounts;
    private final TransactionCategoryRepository categories;
    private final UserRepository users;

    public TransactionService(TransactionRepository transactions,
                              FinancialAccountRepository accounts,
                              TransactionCategoryRepository categories,
                              UserRepository users) {
        this.transactions = transactions;
        this.accounts = accounts;
        this.categories = categories;
        this.users = users;
    }

    @Transactional
    public TransactionResponse create(Long userId, CreateTransactionRequest req) {
        var account = accounts.findByIdAndUserId(req.accountId(), userId)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        var user = users.findById(userId).orElseThrow();
        var cat = req.categoryId() == null ? null :
                categories.findByIdAndUserId(req.categoryId(), userId)
                        .orElseThrow(() -> new NotFoundException("Categoria não encontrada"));

        var tx = Transaction.builder()
                .user(user).account(account).category(cat)
                .type(req.type()).direction(req.direction())
                .amount(req.amount()).transactionDate(req.transactionDate())
                .description(req.description().trim())
                .source(TransactionSource.MANUAL)
                .status(TransactionStatus.POSTED)
                .notes(req.notes())
                .build();
        transactions.save(tx);
        recalculateBalance(account.getId());
        return TransactionResponse.from(tx);
    }

    @Transactional
    public void createTransfer(Long userId, CreateTransferRequest req) {
        if (req.sourceAccountId().equals(req.destinationAccountId())) {
            throw new BusinessException("Conta de origem e destino devem ser diferentes.");
        }
        var src = accounts.findByIdAndUserId(req.sourceAccountId(), userId)
                .orElseThrow(() -> new NotFoundException("Conta de origem não encontrada"));
        var dst = accounts.findByIdAndUserId(req.destinationAccountId(), userId)
                .orElseThrow(() -> new NotFoundException("Conta de destino não encontrada"));
        var user = users.findById(userId).orElseThrow();

        var debit = Transaction.builder()
                .user(user).account(src).type(TransactionType.TRANSFER)
                .direction(TransactionDirection.DEBIT).amount(req.amount())
                .transactionDate(req.transactionDate())
                .description(req.description() + " (saída)").source(TransactionSource.MANUAL)
                .status(TransactionStatus.POSTED).build();
        transactions.save(debit);

        var credit = Transaction.builder()
                .user(user).account(dst).type(TransactionType.TRANSFER)
                .direction(TransactionDirection.CREDIT).amount(req.amount())
                .transactionDate(req.transactionDate())
                .description(req.description() + " (entrada)").source(TransactionSource.MANUAL)
                .status(TransactionStatus.POSTED).build();
        transactions.save(credit);

        debit.setTransferPairId(credit.getId());
        credit.setTransferPairId(debit.getId());

        recalculateBalance(src.getId());
        recalculateBalance(dst.getId());
    }

    @Transactional
    public void delete(Long userId, Long id) {
        var tx = transactions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada"));

        // Remove par de transferência, se existir
        if (tx.getTransferPairId() != null) {
            transactions.findByIdAndUserId(tx.getTransferPairId(), userId)
                    .ifPresent(pair -> {
                        Long otherAccountId = pair.getAccount().getId();
                        transactions.delete(pair);
                        recalculateBalance(otherAccountId);
                    });
        }
        Long accountId = tx.getAccount().getId();
        transactions.delete(tx);
        recalculateBalance(accountId);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listRecent(Long userId) {
        var all = transactions.findAll(
                (root, q, cb) -> cb.equal(root.get("user").get("id"), userId),
                org.springframework.data.domain.PageRequest.of(0, 50,
                        org.springframework.data.domain.Sort.by("transactionDate").descending()
                                .and(org.springframework.data.domain.Sort.by("id").descending())));
        return all.map(TransactionResponse::from).getContent();
    }

    @Transactional
    public void recalculateBalance(Long accountId) {
        FinancialAccount acc = accounts.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        BigDecimal delta = transactions.sumDeltaForAccount(accountId);
        acc.setCurrentBalance(acc.getInitialBalance().add(delta));
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<TransactionResponse> list(
            Long userId, int page, int size, Long accountId,
            TransactionType type, java.time.LocalDate from, java.time.LocalDate to, String q) {

        var spec = (org.springframework.data.jpa.domain.Specification<Transaction>) (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (accountId != null) predicates.add(cb.equal(root.get("account").get("id"), accountId));
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), to));
            if (q != null && !q.isBlank())
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + q.toLowerCase() + "%"));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        var pageable = org.springframework.data.domain.PageRequest.of(page, Math.min(size, 100),
                org.springframework.data.domain.Sort.by("transactionDate").descending()
                        .and(org.springframework.data.domain.Sort.by("id").descending()));
        return transactions.findAll(spec, pageable).map(TransactionResponse::from);
    }
}