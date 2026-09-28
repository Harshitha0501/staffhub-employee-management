package com.staffhub.dto;

/** Yearly leave balance for an employee. remaining = quota − used − pending (clamped ≥ 0). */
public record LeaveBalanceResponse(
        int year,
        int quota,
        long used,
        long pending,
        long remaining) {
}
