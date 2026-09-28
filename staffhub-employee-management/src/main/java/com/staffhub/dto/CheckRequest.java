package com.staffhub.dto;

/** Body for POST /api/attendance/check-in and /check-out. employeeId is optional
 *  (ignored for employees, who always act on their own record). */
public record CheckRequest(Long employeeId) {
}
