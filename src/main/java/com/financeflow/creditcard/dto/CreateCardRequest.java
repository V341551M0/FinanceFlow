package com.financeflow.creditcard.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateCardRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 30) String brand,
        @Size(max = 4) String lastDigits,
        @NotNull @DecimalMin("1.00") BigDecimal creditLimit,
        @NotNull @Min(1) @Max(28) Integer closingDay,
        @NotNull @Min(1) @Max(28) Integer dueDay
) {}