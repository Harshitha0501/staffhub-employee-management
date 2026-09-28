package com.staffhub.dto;

/** Response for POST /api/employees — the new record plus the one-time login it provisioned. */
public record EmployeeCreateResponse(
        EmployeeResponse employee,
        String username,
        String password) {
}
