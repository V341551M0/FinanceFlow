package com.financeflow.notification;

import com.financeflow.common.exception.NotFoundException;
import com.financeflow.notification.dto.NotificationResponse;
import com.financeflow.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional
    public void createIfAbsent(User user, NotificationType type, String title,
                               String message, String externalKey) {
        if (notifications.existsByUserIdAndExternalKey(user.getId(), externalKey)) return;
        var n = Notification.builder()
                .user(user).type(type).title(title).message(message)
                .externalKey(externalKey).read(false).build();
        notifications.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId) {
        return notifications.findTop100ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        var n = notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada"));
        n.setRead(true);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notifications.findTop100ByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> !Boolean.TRUE.equals(n.getRead()))
                .forEach(n -> n.setRead(true));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        var n = notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada"));
        notifications.delete(n);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                Boolean.TRUE.equals(n.getRead()), n.getCreatedAt());
    }

    public record NotificationResponse(Long id, NotificationType type, String title,
                                       String message, Boolean read, Instant createdAt) {}
}
