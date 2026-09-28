package com.staffhub.dto;

import java.util.List;

/** Personal dashboard payload for an EMPLOYEE (/api/dashboard/me). */
public record EmployeeDashboardResponse(
        String employeeName,
        String departmentName,
        String role,
        long openTasks,
        long doneTasks,
        long pendingLeaves,
        long approvedLeaves,
        boolean checkedInToday,
        String checkInTime,
        String checkOutTime,
        List<TaskResponse> recentTasks,
        List<AnnouncementResponse> recentAnnouncements,
        int leaveQuota,
        long leaveRemaining) {
}
