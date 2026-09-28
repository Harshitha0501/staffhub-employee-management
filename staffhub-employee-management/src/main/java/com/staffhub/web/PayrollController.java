package com.staffhub.web;

import com.staffhub.dto.PayrollGenerateRequest;
import com.staffhub.dto.PayrollMonthResponse;
import com.staffhub.dto.PayrollResponse;
import com.staffhub.dto.PayrollUpdateRequest;
import com.staffhub.exception.NotFoundException;
import com.staffhub.model.Payroll;
import com.staffhub.model.User;
import com.staffhub.repository.PayrollRepository;
import com.staffhub.service.CurrentUserService;
import com.staffhub.service.PayrollService;
import com.staffhub.service.PayslipPdfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final PayrollService payrollService;
    private final PayrollRepository payrollRepository;
    private final CurrentUserService currentUserService;
    private final PayslipPdfService payslipPdfService;

    public PayrollController(PayrollService payrollService,
                             PayrollRepository payrollRepository,
                             CurrentUserService currentUserService,
                             PayslipPdfService payslipPdfService) {
        this.payrollService = payrollService;
        this.payrollRepository = payrollRepository;
        this.currentUserService = currentUserService;
        this.payslipPdfService = payslipPdfService;
    }

    /** Month board; no month param defaults to the server's current month. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public PayrollMonthResponse month(@RequestParam(required = false) String month) {
        return payrollService.month(month == null || month.isBlank()
                ? YearMonth.now().format(MONTH_FORMAT)
                : month.trim());
    }

    /** The signed-in employee's own payslips, newest month first. */
    @GetMapping("/mine")
    public List<PayrollResponse> mine() {
        User user = currentUserService.current();
        Long employeeId = currentUserService.requireEmployeeId(user);
        return payrollRepository.findByEmployeeIdOrderByPayMonthDesc(employeeId).stream()
                .map(PayrollResponse::from)
                .toList();
    }

    /** Creates missing payslips for all ACTIVE employees, then returns the fresh month view. */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Map<String, Object> generate(@Valid @RequestBody PayrollGenerateRequest request) {
        int generated = payrollService.generate(request.month()).size();
        return Map.of(
                "generated", generated,
                "message", generated == 0
                        ? "All active employees already have payslips for " + request.month()
                        : "Generated " + generated + " payslip" + (generated == 1 ? "" : "s") + " for " + request.month(),
                "month", payrollService.month(request.month()));
    }

    /** Download a single payslip as a PDF. Employees may download only their own. */
    @GetMapping("/{id}/payslip.pdf")
    public ResponseEntity<byte[]> payslip(@PathVariable Long id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payslip not found"));
        User user = currentUserService.current();
        if (!currentUserService.isStaff(user)) {
            Long myEmployeeId = currentUserService.requireEmployeeId(user);
            if (!payroll.getEmployee().getId().equals(myEmployeeId)) {
                throw new NotFoundException("Payslip not found");
            }
        }
        byte[] pdf = payslipPdfService.build(payroll);
        String filename = "payslip-" + payroll.getEmployee().getEmployeeCode() + "-" + payroll.getPayMonth() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public PayrollResponse update(@PathVariable Long id, @Valid @RequestBody PayrollUpdateRequest request) {
        return PayrollResponse.from(payrollService.update(id, request));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public PayrollResponse markPaid(@PathVariable Long id) {
        return PayrollResponse.from(payrollService.markPaid(id));
    }
}
