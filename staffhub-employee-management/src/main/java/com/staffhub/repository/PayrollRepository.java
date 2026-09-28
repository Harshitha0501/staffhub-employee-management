package com.staffhub.repository;

import com.staffhub.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    List<Payroll> findByPayMonthOrderByEmployeeIdAsc(String payMonth);

    Optional<Payroll> findByEmployeeIdAndPayMonth(Long employeeId, String payMonth);

    List<Payroll> findByEmployeeId(Long employeeId);

    List<Payroll> findByEmployeeIdOrderByPayMonthDesc(Long employeeId);
}
