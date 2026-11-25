package com.financeflow.notification.dto;

import com.financeflow.notification.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Boolean read,
        Instant createdAt
) {}
