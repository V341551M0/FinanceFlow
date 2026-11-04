package com.financeflow.goal.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGoalRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.01") BigDecimal targetAmount,
        LocalDate targetDate
) {}