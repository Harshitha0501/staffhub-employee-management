package com.staffhub.dto;

import com.staffhub.model.Task;
import com.staffhub.model.TaskPriority;
import com.staffhub.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String departmentName,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        String createdByName,
        boolean selfCreated,
        LocalDateTime createdAt) {

    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getEmployee().getId(),
                t.getEmployee().getFullName(),
                t.getEmployee().getDepartment().getName(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getPriority(),
                t.getDueDate(),
                t.getCreatedByName(),
                t.isSelfCreated(),
                t.getCreatedAt());
    }
}
