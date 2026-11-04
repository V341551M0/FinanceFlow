package com.financeflow.transaction.dto;

import com.financeflow.transaction.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName,
        TransactionType type,
        TransactionDirection direction,
        BigDecimal amount,
        LocalDate transactionDate,
        String description,
        TransactionSource source,
        TransactionStatus status,
        String notes,
        Long transferPairId,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAccount().getId(),
                t.getAccount().getName(),
                t.getCategory() == null ? null : t.getCategory().getId(),
                t.getCategory() == null ? null : t.getCategory().getName(),
                t.getType(), t.getDirection(), t.getAmount(),
                t.getTransactionDate(), t.getDescription(),
                t.getSource(), t.getStatus(), t.getNotes(),
                t.getTransferPairId(), t.getCreatedAt());
    }
}