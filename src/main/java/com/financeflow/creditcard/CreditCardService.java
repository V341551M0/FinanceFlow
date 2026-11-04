package com.financeflow.creditcard;

import com.financeflow.common.exception.BusinessException;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.creditcard.dto.*;
import com.financeflow.transaction.TransactionCategory;
import com.financeflow.transaction.TransactionCategoryRepository;
import com.financeflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class CreditCardService {

    private final CreditCardRepository cards;
    private final CreditCardInvoiceRepository invoices;
    private final CreditCardTransactionRepository purchases;
    private final TransactionCategoryRepository categories;
    private final UserRepository users;

    public CreditCardService(CreditCardRepository cards,
                             CreditCardInvoiceRepository invoices,
                             CreditCardTransactionRepository purchases,
                             TransactionCategoryRepository categories,
                             UserRepository users) {
        this.cards = cards;
        this.invoices = invoices;
        this.purchases = purchases;
        this.categories = categories;
        this.users = users;
    }

    @Transactional
    public CardResponse create(Long userId, CreateCardRequest req) {
        var user = users.findById(userId).orElseThrow();
        var card = CreditCard.builder()
                .user(user)
                .name(req.name().trim())
                .brand(req.brand().trim())
                .lastDigits(req.lastDigits())
                .creditLimit(req.creditLimit())
                .closingDay(req.closingDay())
                .dueDay(req.dueDay())
                .active(true)
                .build();
        cards.save(card);
        return CardResponse.from(card, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<CardResponse> list(Long userId) {
        return cards.findAllByUserIdAndActiveTrueOrderByNameAsc(userId).stream()
                .map(c -> CardResponse.from(c, usedLimit(c.getId())))
                .toList();
    }

    @Transactional
    public void deactivate(Long userId, Long cardId) {
        var card = cards.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Cartão não encontrado"));
        card.setActive(false);
    }

    /**
     * Compra: gera 1 compra ou N parcelas, alocando cada uma na fatura do mês correspondente.
     */
    @Transactional
    public void addPurchase(Long userId, Long cardId, AddPurchaseRequest req) {
        var card = cards.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Cartão não encontrado"));

        TransactionCategory cat = null;
        if (req.categoryId() != null) {
            cat = categories.findByIdAndUserId(req.categoryId(), userId)
                    .orElseThrow(() -> new NotFoundException("Categoria não encontrada"));
        }

        int n = req.installments();
        BigDecimal installmentAmount = req.amount()
                .divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        // Ajuste de centavos na primeira parcela
        BigDecimal totalDistributed = installmentAmount.multiply(BigDecimal.valueOf(n));
        BigDecimal adjustment = req.amount().subtract(totalDistributed);

        Long groupId = System.currentTimeMillis(); // ID único de parcelamento

        for (int i = 0; i < n; i++) {
            LocalDate purchaseDate = req.transactionDate().plusMonths(i);
            var invoice = getOrCreateInvoice(card, purchaseDate);

            BigDecimal amount = installmentAmount;
            if (i == 0) amount = amount.add(adjustment);

            var purchase = CreditCardTransaction.builder()
                    .invoice(invoice)
                    .category(cat)
                    .description(n > 1
                            ? req.description() + " (" + (i + 1) + "/" + n + ")"
                            : req.description())
                    .amount(amount)
                    .transactionDate(purchaseDate)
                    .installmentNumber(n > 1 ? i + 1 : null)
                    .installmentTotal(n > 1 ? n : null)
                    .installmentGroupId(n > 1 ? groupId : null)
                    .build();
            purchases.save(purchase);

            invoice.setTotalAmount(invoice.getTotalAmount().add(amount));
        }
    }

    /**
     * Pagar fatura: marca como PAID e registra a despesa na conta corrente.
     */
    @Transactional
    public void payInvoice(Long userId, Long invoiceId, Long accountId,
                           com.financeflow.transaction.TransactionService txService) {
        var invoice = invoices.findByIdAndCardUserId(invoiceId, userId)
                .orElseThrow(() -> new NotFoundException("Fatura não encontrada"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("Fatura já está paga.");
        }
        if (invoice.getTotalAmount().signum() == 0) {
            throw new BusinessException("Fatura sem valor a pagar.");
        }

        txService.create(userId, new com.financeflow.transaction.dto.CreateTransactionRequest(
                accountId, null,
                com.financeflow.transaction.TransactionType.EXPENSE,
                com.financeflow.transaction.TransactionDirection.DEBIT,
                invoice.getTotalAmount(),
                invoice.getDueDate(),
                "Pagamento fatura " + invoice.getCard().getName()
                        + " (" + invoice.getReferenceMonth() + ")",
                null));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(java.time.Instant.now());
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(Long userId, Long cardId) {
        cards.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Cartão não encontrado"));
        return invoices.findAllByCardIdOrderByReferenceMonthDesc(cardId).stream()
                .map(InvoiceResponse::from).toList();
    }

    private BigDecimal usedLimit(Long cardId) {
        // Soma de compras no cartão em faturas não pagas
        return invoices.findAllByCardIdOrderByReferenceMonthDesc(cardId).stream()
                .filter(i -> i.getStatus() != InvoiceStatus.PAID)
                .map(CreditCardInvoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CreditCardInvoice getOrCreateInvoice(CreditCard card, LocalDate refDate) {
        LocalDate referenceMonth = refDate.withDayOfMonth(1);
        return invoices.findByCardIdAndReferenceMonth(card.getId(), referenceMonth)
                .orElseGet(() -> {
                    var closing = LocalDate.of(referenceMonth.getYear(),
                            referenceMonth.getMonth(), card.getClosingDay());
                    var dueBase = closing.plusMonths(1);
                    var due = LocalDate.of(dueBase.getYear(), dueBase.getMonth(), card.getDueDay());
                    return invoices.save(CreditCardInvoice.builder()
                            .card(card)
                            .referenceMonth(referenceMonth)
                            .closingDate(closing)
                            .dueDate(due)
                            .totalAmount(BigDecimal.ZERO)
                            .status(InvoiceStatus.OPEN)
                            .build());
                });
    }
}