package com.staffhub.dto;

import com.staffhub.model.Payroll;

import java.math.BigDecimal;

public record PayrollResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        String departmentName,
        String role,
        String month,
        BigDecimal baseSalary,
        BigDecimal bonus,
        BigDecimal deductions,
        BigDecimal netPay,
        String status) {

    public static PayrollResponse from(Payroll payroll) {
        return new PayrollResponse(
                payroll.getId(),
                payroll.getEmployee().getId(),
                payroll.getEmployee().getEmployeeCode(),
                payroll.getEmployee().getFullName(),
                payroll.getEmployee().getDepartment().getName(),
                payroll.getEmployee().getRole(),
                payroll.getPayMonth(),
                payroll.getBaseSalary(),
                payroll.getBonus(),
                payroll.getDeductions(),
                payroll.getNetPay(),
                payroll.getStatus().name());
    }
}
