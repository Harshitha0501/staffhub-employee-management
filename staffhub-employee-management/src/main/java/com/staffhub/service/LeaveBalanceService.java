package com.staffhub.service;

import com.staffhub.dto.LeaveBalanceResponse;
import com.staffhub.model.LeaveRequest;
import com.staffhub.model.LeaveStatus;
import com.staffhub.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;

/**
 * Computes yearly leave usage against a fixed annual quota
 * (staffhub.leave.annual-quota, default 24 working days).
 */
@Service
public class LeaveBalanceService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final int annualQuota;

    public LeaveBalanceService(LeaveRequestRepository leaveRequestRepository,
                               @Value("${staffhub.leave.annual-quota:24}") int annualQuota) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.annualQuota = annualQuota;
    }

    public int quota() {
        return annualQuota;
    }

    public LeaveBalanceResponse balance(Long employeeId, int year) {
        long used = 0;
        long pending = 0;
        for (LeaveRequest leave : leaveRequestRepository.findByEmployeeId(employeeId)) {
            if (leave.getStartDate().getYear() != year) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
            if (leave.getStatus() == LeaveStatus.APPROVED) {
                used += days;
            } else if (leave.getStatus() == LeaveStatus.PENDING) {
                pending += days;
            }
        }
        long remaining = Math.max(0, annualQuota - used - pending);
        return new LeaveBalanceResponse(year, annualQuota, used, pending, remaining);
    }
}
