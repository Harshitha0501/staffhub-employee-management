package com.staffhub.dto;

import com.staffhub.model.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body for POST/PUT /api/tasks. employeeId is honoured only for ADMIN/HR;
 * employees always create tasks for themselves.
 */
public record TaskRequest(
        Long employeeId,
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters") String title,
        @Size(max = 1000, message = "Description must be at most 1000 characters") String description,
        TaskPriority priority,
        LocalDate dueDate) {
}
