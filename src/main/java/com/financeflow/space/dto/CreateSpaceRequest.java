package com.financeflow.space.dto;

import com.financeflow.space.SpaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSpaceRequest(
        @NotBlank(message = "Nome do espaço é obrigatório")
        @Size(max = 120)
        String name,

        @NotNull(message = "Tipo do espaço é obrigatório")
        SpaceType type,

        @Size(max = 500)
        String description,

        @Size(min = 3, max = 3)
        String currency,

        @Size(max = 7)
        String color,

        @Size(max = 40)
        String icon,

        Long defaultAccountId
) {}
