package com.financeflow.goal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ContributeRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) {}