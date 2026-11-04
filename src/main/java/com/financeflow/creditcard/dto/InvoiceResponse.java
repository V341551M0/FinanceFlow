package com.financeflow.creditcard.dto;

import com.financeflow.creditcard.CreditCardInvoice;
import com.financeflow.creditcard.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceResponse(
        Long id, Long cardId, LocalDate referenceMonth,
        LocalDate closingDate, LocalDate dueDate,
        BigDecimal totalAmount, InvoiceStatus status
) {
    public static InvoiceResponse from(CreditCardInvoice i) {
        return new InvoiceResponse(
                i.getId(), i.getCard().getId(), i.getReferenceMonth(),
                i.getClosingDate(), i.getDueDate(),
                i.getTotalAmount(), i.getStatus());
    }
}