package com.financeflow.ledger.dto;

import jakarta.validation.constraints.NotBlank;

public record ReverseEntryRequest(
        @NotBlank(message = "Chave de idempotência é obrigatória")
        String idempotencyKey,
        String reason
) {}
