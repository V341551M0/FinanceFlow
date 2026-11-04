package com.financeflow.transaction.dto;

import com.financeflow.transaction.TransactionType;
import jakarta.validation.constraints.*;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull TransactionType type,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "cor no formato #RRGGBB") String color,
        @Size(max = 40) String icon
) {}