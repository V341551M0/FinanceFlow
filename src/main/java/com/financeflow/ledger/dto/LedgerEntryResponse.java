package com.financeflow.ledger.dto;

import com.financeflow.ledger.JournalEntry;
import com.financeflow.ledger.JournalEntryStatus;
import com.financeflow.ledger.PostingSide;
import com.financeflow.ledger.RecordOrigin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LedgerEntryResponse(
        Long id,
        UUID entryUuid,
        String idempotencyKey,
        LocalDate entryDate,
        LocalDate effectiveDate,
        String description,
        String referenceType,
        Long referenceId,
        RecordOrigin recordOrigin,
        JournalEntryStatus status,
        Long reversalOfId,
        List<PostingLineResponse> postings
) {
    public record PostingLineResponse(
            Long id,
            Long ledgerAccountId,
            String ledgerAccountCode,
            String ledgerAccountName,
            PostingSide side,
            BigDecimal amount,
            String currency,
            String memo
    ) {}

    public static LedgerEntryResponse from(JournalEntry entry) {
        var lines = entry.getPostings().stream()
                .map(p -> new PostingLineResponse(
                        p.getId(),
                        p.getLedgerAccount().getId(),
                        p.getLedgerAccount().getCode(),
                        p.getLedgerAccount().getName(),
                        p.getSide(),
                        p.getAmount(),
                        p.getCurrency(),
                        p.getMemo()
                ))
                .toList();

        return new LedgerEntryResponse(
                entry.getId(),
                entry.getEntryUuid(),
                entry.getIdempotencyKey(),
                entry.getEntryDate(),
                entry.getEffectiveDate(),
                entry.getDescription(),
                entry.getReferenceType(),
                entry.getReferenceId(),
                entry.getRecordOrigin(),
                entry.getStatus(),
                entry.getReversalOf() != null ? entry.getReversalOf().getId() : null,
                lines
        );
    }
}
