package com.staffhub.dto;

import com.staffhub.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

/** Body for PUT /api/tasks/{id}/status. */
public record TaskStatusRequest(
        @NotNull(message = "Status is required") TaskStatus status) {
}
