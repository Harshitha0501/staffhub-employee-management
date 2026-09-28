package com.staffhub.dto;

import com.staffhub.model.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        String type,
        boolean read,
        LocalDateTime createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(),
                n.getType(), n.isRead(), n.getCreatedAt());
    }
}
