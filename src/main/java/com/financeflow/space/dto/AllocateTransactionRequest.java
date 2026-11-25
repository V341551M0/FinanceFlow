package com.financeflow.space.dto;

import jakarta.validation.constraints.NotNull;

public record AllocateTransactionRequest(
        @NotNull(message = "Espaço de destino é obrigatório")
        Long spaceId
) {}
