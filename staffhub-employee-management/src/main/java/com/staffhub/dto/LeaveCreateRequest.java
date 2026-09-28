package com.staffhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Body for POST /api/leaves. */
public record LeaveCreateRequest(
        @NotNull(message = "Employee is required") Long employeeId,
        @NotNull(message = "Start date is required") LocalDate startDate,
        @NotNull(message = "End date is required") LocalDate endDate,
        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must be at most 500 characters") String reason) {
}
