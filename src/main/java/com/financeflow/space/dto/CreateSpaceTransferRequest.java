package com.financeflow.space.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSpaceTransferRequest(
        @NotNull(message = "Espaço de origem é obrigatório")
        Long sourceSpaceId,

        @NotNull(message = "Espaço de destino é obrigatório")
        Long destinationSpaceId,

        @NotNull(message = "Valor é obrigatório")
        @Positive(message = "Valor deve ser positivo")
        BigDecimal amount,

        @NotNull(message = "Data da transferência é obrigatória")
        LocalDate transferDate,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 255)
        String description,

        String idempotencyKey
) {}
