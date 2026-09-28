package com.staffhub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Body for PUT /api/payroll/{id} — adjusts bonus/deductions, net pay is recomputed. */
public record PayrollUpdateRequest(
        @NotNull(message = "Bonus is required")
        @DecimalMin(value = "0.0", message = "Bonus cannot be negative") BigDecimal bonus,
        @NotNull(message = "Deductions are required")
        @DecimalMin(value = "0.0", message = "Deductions cannot be negative") BigDecimal deductions) {
}
