package com.staffhub.repository;

import com.staffhub.model.LeaveRequest;
import com.staffhub.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findAllByOrderByAppliedAtDesc();

    List<LeaveRequest> findByStatusOrderByAppliedAtDesc(LeaveStatus status);

    List<LeaveRequest> findByEmployeeId(Long employeeId);

    long countByStatus(LeaveStatus status);
}
