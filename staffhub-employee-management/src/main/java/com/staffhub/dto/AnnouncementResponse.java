package com.staffhub.dto;

import com.staffhub.model.Announcement;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        String title,
        String body,
        String authorName,
        LocalDateTime createdAt) {

    public static AnnouncementResponse from(Announcement a) {
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getBody(),
                a.getAuthorName(), a.getCreatedAt());
    }
}
