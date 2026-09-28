package com.staffhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Body for POST /api/payroll/generate. month is yyyy-MM. */
public record PayrollGenerateRequest(
        @NotBlank(message = "Month is required")
        @Pattern(regexp = "\\d{4}-\\d{2}", message = "Month must use the yyyy-MM format") String month) {
}
