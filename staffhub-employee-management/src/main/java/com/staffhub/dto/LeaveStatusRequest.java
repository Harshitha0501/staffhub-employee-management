package com.staffhub.dto;

import com.staffhub.model.LeaveStatus;
import jakarta.validation.constraints.NotNull;

/** Body for PUT /api/leaves/{id}/status. */
public record LeaveStatusRequest(@NotNull(message = "Status is required") LeaveStatus status) {
}
