package com.financeflow.account.dto;

import com.financeflow.account.AccountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String institution,
        @NotNull AccountType type,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull @DecimalMin("0.00") BigDecimal initialBalance
) {}