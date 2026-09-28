package com.staffhub.dto;

import java.math.BigDecimal;
import java.util.List;

/** GET /api/payroll?month=yyyy-MM — rows plus pre-computed totals for the summary cards. */
public record PayrollMonthResponse(
        String month,
        List<PayrollResponse> records,
        BigDecimal totalBase,
        BigDecimal totalBonus,
        BigDecimal totalDeductions,
        BigDecimal totalNet,
        long paidCount,
        long pendingCount) {
}
