package com.staffhub.dto;

import com.staffhub.model.LeaveRequest;
import com.staffhub.model.LeaveStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record LeaveResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String departmentName,
        LocalDate startDate,
        LocalDate endDate,
        long days,
        String reason,
        LeaveStatus status,
        LocalDateTime appliedAt) {

    public static LeaveResponse from(LeaveRequest leave) {
        long days = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
        return new LeaveResponse(
                leave.getId(),
                leave.getEmployee().getId(),
                leave.getEmployee().getFullName(),
                leave.getEmployee().getDepartment().getName(),
                leave.getStartDate(),
                leave.getEndDate(),
                days,
                leave.getReason(),
                leave.getStatus(),
                leave.getAppliedAt());
    }
}
