package com.financeflow.space.dto;

import com.financeflow.space.SpaceTransfer;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SpaceTransferResponse(
        Long id,
        Long sourceSpaceId,
        String sourceSpaceName,
        Long destinationSpaceId,
        String destinationSpaceName,
        BigDecimal amount,
        String currency,
        LocalDate transferDate,
        String description,
        Long journalEntryId
) {
    public static SpaceTransferResponse from(SpaceTransfer st) {
        return new SpaceTransferResponse(
                st.getId(),
                st.getSourceSpace().getId(),
                st.getSourceSpace().getName(),
                st.getDestinationSpace().getId(),
                st.getDestinationSpace().getName(),
                st.getAmount(),
                st.getCurrency(),
                st.getTransferDate(),
                st.getDescription(),
                st.getJournalEntry() != null ? st.getJournalEntry().getId() : null
        );
    }
}
