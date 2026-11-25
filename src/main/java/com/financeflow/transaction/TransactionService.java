package com.financeflow.transaction;

import com.financeflow.account.FinancialAccount;
import com.financeflow.account.FinancialAccountRepository;
import com.financeflow.common.exception.BusinessException;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.ledger.DoubleEntryLedgerService;
import com.financeflow.ledger.DoubleEntryLedgerService.PostEntryCommand;
import com.financeflow.ledger.DoubleEntryLedgerService.PostingLine;
import com.financeflow.ledger.JournalEntryStatus;
import com.financeflow.ledger.LedgerAccount;
import com.financeflow.ledger.LedgerAccountClass;
import com.financeflow.ledger.PostingSide;
import com.financeflow.ledger.RecordOrigin;
import com.financeflow.transaction.dto.CreateTransactionRequest;
import com.financeflow.transaction.dto.CreateTransferRequest;
import com.financeflow.transaction.dto.TransactionResponse;
import com.financeflow.user.User;
import com.financeflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final FinancialAccountRepository accounts;
    private final TransactionCategoryRepository categories;
    private final UserRepository users;
    private final DoubleEntryLedgerService ledger;

    public TransactionService(TransactionRepository transactions,
                              FinancialAccountRepository accounts,
                              TransactionCategoryRepository categories,
                              UserRepository users,
                              DoubleEntryLedgerService ledger) {
        this.transactions = transactions;
        this.accounts = accounts;
        this.categories = categories;
        this.users = users;
        this.ledger = ledger;
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
        recordTransactionInLedger(user, account, cat, tx);
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

        recordTransferInLedger(user, src, dst, debit, credit, req);

        recalculateBalance(src.getId());
        recalculateBalance(dst.getId());
    }

    @Transactional
    public void delete(Long userId, Long id) {
        var tx = transactions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada"));

        if (tx.getTransferPairId() != null) {
            Long minId = Math.min(tx.getId(), tx.getTransferPairId());
            Long maxId = Math.max(tx.getId(), tx.getTransferPairId());
            ledger.reverseByIdempotencyKeyIfPresent(
                    userId,
                    "TRANSFER:" + minId + ":" + maxId,
                    "REV_TRANSFER:" + minId + ":" + maxId,
                    "Estorno de transferência #" + minId
            );

            transactions.findByIdAndUserId(tx.getTransferPairId(), userId)
                    .ifPresent(pair -> {
                        Long otherAccountId = pair.getAccount().getId();
                        transactions.delete(pair);
                        recalculateBalance(otherAccountId);
                    });
        } else if (tx.getId() != null) {
            ledger.reverseByIdempotencyKeyIfPresent(
                    userId,
                    "TX:" + tx.getId(),
                    "REV_TX:" + tx.getId(),
                    "Estorno de transação #" + tx.getId()
            );
        }

        Long accountId = tx.getAccount().getId();
        transactions.delete(tx);
        recalculateBalance(accountId);
    }

    private void recordTransactionInLedger(User user, FinancialAccount account,
                                           TransactionCategory category, Transaction tx) {
        if (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        LedgerAccount assetAcc = ledger.ensureAccountForFinancialAccount(account);
        if (assetAcc == null) {
            return;
        }

        String currency = account.getCurrency() != null ? account.getCurrency() : "BRL";
        boolean isCreditToAsset = tx.getDirection() == TransactionDirection.CREDIT;
        LedgerAccountClass counterClass = isCreditToAsset ? LedgerAccountClass.REVENUE : LedgerAccountClass.EXPENSE;
        String counterCode = category != null
                ? counterClass.name() + ":CAT:" + category.getId() + ":" + currency
                : counterClass.name() + ":GENERAL:" + currency;
        String counterName = category != null
                ? category.getName()
                : (isCreditToAsset ? "Receitas Gerais (" + currency + ")" : "Despesas Gerais (" + currency + ")");

        LedgerAccount counterAcc = ledger.ensureSystemAccount(user, counterCode, counterName, counterClass, currency);
        if (counterAcc == null) {
            return;
        }

        List<PostingLine> lines = isCreditToAsset
                ? List.of(
                        new PostingLine(assetAcc.getId(), PostingSide.DEBIT, tx.getAmount(), tx.getDescription()),
                        new PostingLine(counterAcc.getId(), PostingSide.CREDIT, tx.getAmount(), tx.getDescription())
                )
                : List.of(
                        new PostingLine(counterAcc.getId(), PostingSide.DEBIT, tx.getAmount(), tx.getDescription()),
                        new PostingLine(assetAcc.getId(), PostingSide.CREDIT, tx.getAmount(), tx.getDescription())
                );

        String idempotencyKey = tx.getId() != null ? "TX:" + tx.getId() : "TX:" + UUID.randomUUID();
        ledger.postEntry(new PostEntryCommand(
                user,
                idempotencyKey,
                tx.getTransactionDate(),
                tx.getTransactionDate(),
                tx.getDescription(),
                "TRANSACTION",
                tx.getId(),
                RecordOrigin.INTERNAL,
                JournalEntryStatus.POSTED,
                lines
        ));
    }

    private void recordTransferInLedger(User user, FinancialAccount src, FinancialAccount dst,
                                        Transaction debit, Transaction credit, CreateTransferRequest req) {
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        LedgerAccount srcLedger = ledger.ensureAccountForFinancialAccount(src);
        LedgerAccount dstLedger = ledger.ensureAccountForFinancialAccount(dst);
        if (srcLedger == null || dstLedger == null) {
            return;
        }

        String idempotencyKey = (debit.getId() != null && credit.getId() != null)
                ? "TRANSFER:" + Math.min(debit.getId(), credit.getId()) + ":" + Math.max(debit.getId(), credit.getId())
                : "TRANSFER:" + UUID.randomUUID();

        ledger.postEntry(new PostEntryCommand(
                user,
                idempotencyKey,
                req.transactionDate(),
                req.transactionDate(),
                req.description(),
                "TRANSFER",
                debit.getId(),
                RecordOrigin.INTERNAL,
                JournalEntryStatus.POSTED,
                List.of(
                        new PostingLine(dstLedger.getId(), PostingSide.DEBIT, req.amount(), "Entrada transferência"),
                        new PostingLine(srcLedger.getId(), PostingSide.CREDIT, req.amount(), "Saída transferência")
                )
        ));
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
