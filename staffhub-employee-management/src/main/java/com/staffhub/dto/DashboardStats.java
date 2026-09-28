package com.staffhub.dto;

import java.math.BigDecimal;
import java.util.List;

/** GET /api/dashboard/stats — the numbers powering the dashboard bento grid. */
public record DashboardStats(
        long totalEmployees,
        long activeEmployees,
        long inactiveEmployees,
        long totalDepartments,
        long presentToday,
        long pendingLeaves,
        BigDecimal monthlyPayroll,
        String monthlyPayrollMonth,
        List<EmployeeResponse> recentHires) {
}
