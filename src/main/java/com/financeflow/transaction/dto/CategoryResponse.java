package com.financeflow.transaction.dto;

import com.financeflow.transaction.TransactionCategory;
import com.financeflow.transaction.TransactionType;

public record CategoryResponse(Long id, String name, TransactionType type, String color, String icon) {
    public static CategoryResponse from(TransactionCategory c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getType(), c.getColor(), c.getIcon());
    }
}