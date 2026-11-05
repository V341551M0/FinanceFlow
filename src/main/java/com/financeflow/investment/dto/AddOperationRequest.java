package com.financeflow.investment.dto;

import com.financeflow.investment.OperationType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddOperationRequest(
        @NotNull OperationType type,
        @NotNull @DecimalMin("0.00000001") BigDecimal quantity,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull LocalDate operationDate,
        @DecimalMin("0.00") BigDecimal fees,
        @Size(max = 255) String notes
) {}