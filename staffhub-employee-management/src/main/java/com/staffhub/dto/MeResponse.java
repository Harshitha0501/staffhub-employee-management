package com.staffhub.dto;

import com.staffhub.model.User;

/** Current-user payload returned by /api/auth/me and login. */
public record MeResponse(
        String username,
        String fullName,
        String role,
        String email,
        Long employeeId,
        String employeeCode,
        String departmentName) {

    public static MeResponse from(User user) {
        var employee = user.getEmployee();
        return new MeResponse(
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getEmail(),
                employee == null ? null : employee.getId(),
                employee == null ? null : employee.getEmployeeCode(),
                employee == null ? null : employee.getDepartment().getName());
    }
}
