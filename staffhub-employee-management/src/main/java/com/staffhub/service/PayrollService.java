package com.staffhub.service;

import com.staffhub.dto.PayrollMonthResponse;
import com.staffhub.dto.PayrollResponse;
import com.staffhub.dto.PayrollUpdateRequest;
import com.staffhub.exception.NotFoundException;
import com.staffhub.model.Employee;
import com.staffhub.model.EmployeeStatus;
import com.staffhub.model.Payroll;
import com.staffhub.model.PayrollStatus;
import com.staffhub.repository.EmployeeRepository;
import com.staffhub.repository.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Monthly payroll: one payslip per active employee per month.
 * Net pay = base salary + bonus - deductions.
 */
@Service
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;

    public PayrollService(PayrollRepository payrollRepository,
                          EmployeeRepository employeeRepository) {
        this.payrollRepository = payrollRepository;
        this.employeeRepository = employeeRepository;
    }

    public PayrollMonthResponse month(String payMonth) {
        List<PayrollResponse> records = payrollRepository.findByPayMonthOrderByEmployeeIdAsc(payMonth)
                .stream()
                .map(PayrollResponse::from)
                .toList();

        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalBonus = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        long paidCount = 0;
        for (PayrollResponse record : records) {
            totalBase = totalBase.add(record.baseSalary());
            totalBonus = totalBonus.add(record.bonus());
            totalDeductions = totalDeductions.add(record.deductions());
            totalNet = totalNet.add(record.netPay());
            if ("PAID".equals(record.status())) {
                paidCount++;
            }
        }
        return new PayrollMonthResponse(payMonth, records, totalBase, totalBonus, totalDeductions,
                totalNet, paidCount, records.size() - paidCount);
    }

    /** Creates payslips for every ACTIVE employee that does not have one for the month yet. */
    @Transactional
    public List<Payroll> generate(String payMonth) {
        List<Payroll> created = new ArrayList<>();
        for (Employee employee : employeeRepository.findByStatus(EmployeeStatus.ACTIVE)) {
            if (payrollRepository.findByEmployeeIdAndPayMonth(employee.getId(), payMonth).isPresent()) {
                continue;
            }
            created.add(payrollRepository.save(new Payroll(employee, payMonth)));
        }
        return created;
    }

    @Transactional
    public Payroll update(Long id, PayrollUpdateRequest request) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payslip not found"));
        if (payroll.getStatus() == PayrollStatus.PAID) {
            throw new IllegalStateException("This payslip is already paid and can no longer be edited");
        }
        BigDecimal net = payroll.getBaseSalary().add(request.bonus()).subtract(request.deductions());
        if (net.signum() < 0) {
            throw new IllegalArgumentException("Deductions cannot exceed base salary plus bonus");
        }
        payroll.setBonus(request.bonus());
        payroll.setDeductions(request.deductions());
        payroll.setNetPay(net);
        return payrollRepository.save(payroll);
    }

    @Transactional
    public Payroll markPaid(Long id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payslip not found"));
        payroll.setStatus(PayrollStatus.PAID);
        return payrollRepository.save(payroll);
    }
}
