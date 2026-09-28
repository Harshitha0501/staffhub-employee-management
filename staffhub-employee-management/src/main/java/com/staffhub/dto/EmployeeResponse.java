package com.staffhub.dto;

import com.staffhub.model.Employee;
import com.staffhub.model.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Employee shape returned to the frontend (department flattened to id + name). */
public record EmployeeResponse(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        String phone,
        Long departmentId,
        String departmentName,
        String role,
        BigDecimal salary,
        LocalDate hireDate,
        EmployeeStatus status) {

    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(
                e.getId(),
                e.getEmployeeCode(),
                e.getFirstName(),
                e.getLastName(),
                e.getEmail(),
                e.getPhone(),
                e.getDepartment().getId(),
                e.getDepartment().getName(),
                e.getRole(),
                e.getSalary(),
                e.getHireDate(),
                e.getStatus());
    }
}
