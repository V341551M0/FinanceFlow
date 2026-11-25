package com.financeflow.space.dto;

import com.financeflow.space.FinancialSpace;
import com.financeflow.space.SpaceType;

public record SpaceResponse(
        Long id,
        String name,
        String slug,
        SpaceType type,
        String description,
        String currency,
        String color,
        String icon,
        boolean defaultSpace,
        boolean active,
        Long defaultAccountId
) {
    public static SpaceResponse from(FinancialSpace s) {
        return new SpaceResponse(
                s.getId(),
                s.getName(),
                s.getSlug(),
                s.getType(),
                s.getDescription(),
                s.getCurrency(),
                s.getColor(),
                s.getIcon(),
                Boolean.TRUE.equals(s.getDefaultSpace()),
                Boolean.TRUE.equals(s.getActive()),
                s.getDefaultAccount() != null ? s.getDefaultAccount().getId() : null
        );
    }
}
