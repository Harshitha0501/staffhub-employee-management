package com.staffhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for POST /api/announcements. */
public record AnnouncementRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters") String title,
        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message must be at most 2000 characters") String body) {
}
