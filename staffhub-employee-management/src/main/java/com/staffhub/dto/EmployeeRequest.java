package com.staffhub.dto;

import com.staffhub.model.EmployeeStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Body for POST /api/employees and PUT /api/employees/{id}. */
public record EmployeeRequest(
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Last name is required") String lastName,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        String phone,
        @NotNull(message = "Department is required") Long departmentId,
        @NotBlank(message = "Role is required") String role,
        @NotNull(message = "Salary is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be greater than zero") BigDecimal salary,
        @NotNull(message = "Hire date is required") LocalDate hireDate,
        EmployeeStatus status) {
}
