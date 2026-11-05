package com.financeflow.investment.dto;

import com.financeflow.investment.AssetType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInvestmentRequest(
        @NotBlank @Size(max = 20) String symbol,
        @NotBlank @Size(max = 120) String name,
        @NotNull AssetType type,
        @Size(max = 20) String exchange,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull @DecimalMin("0.00000001") BigDecimal quantity,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull LocalDate operationDate,
        @DecimalMin("0.00") BigDecimal fees
) {}