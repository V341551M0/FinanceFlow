package com.financeflow.creditcard.dto;

import com.financeflow.creditcard.CreditCard;

import java.math.BigDecimal;

public record CardResponse(
        Long id, String name, String brand, String lastDigits,
        BigDecimal creditLimit, Integer closingDay, Integer dueDay,
        BigDecimal usedLimit, BigDecimal availableLimit, Boolean active
) {
    public static CardResponse from(CreditCard c, BigDecimal used) {
        return new CardResponse(
                c.getId(), c.getName(), c.getBrand(), c.getLastDigits(),
                c.getCreditLimit(), c.getClosingDay(), c.getDueDay(),
                used, c.getCreditLimit().subtract(used), c.getActive());
    }
}