package com.staffhub.web;

import com.staffhub.dto.LeaveBalanceResponse;
import com.staffhub.dto.LeaveCreateRequest;
import com.staffhub.dto.LeaveResponse;
import com.staffhub.dto.LeaveStatusRequest;
import com.staffhub.exception.NotFoundException;
import com.staffhub.model.Employee;
import com.staffhub.model.LeaveRequest;
import com.staffhub.model.LeaveStatus;
import com.staffhub.model.User;
import com.staffhub.repository.EmployeeRepository;
import com.staffhub.repository.LeaveRequestRepository;
import com.staffhub.service.CurrentUserService;
import com.staffhub.service.LeaveBalanceService;
import com.staffhub.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final LeaveBalanceService leaveBalanceService;

    public LeaveController(LeaveRequestRepository leaveRequestRepository,
                           EmployeeRepository employeeRepository,
                           CurrentUserService currentUserService,
                           NotificationService notificationService,
                           LeaveBalanceService leaveBalanceService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.leaveBalanceService = leaveBalanceService;
    }

    /** Current-year leave balance. Employees see their own; staff pass ?employeeId=. */
    @GetMapping("/balance")
    public LeaveBalanceResponse balance(@RequestParam(required = false) Long employeeId) {
        User user = currentUserService.current();
        Long targetId = currentUserService.isStaff(user) ? employeeId : currentUserService.requireEmployeeId(user);
        if (targetId == null) {
            throw new IllegalArgumentException("employeeId is required");
        }
        return leaveBalanceService.balance(targetId, java.time.Year.now().getValue());
    }

    /** Staff see everyone's requests (optionally by status); employees see only their own. */
    @GetMapping
    public List<LeaveResponse> list(@RequestParam(required = false) LeaveStatus status) {
        User user = currentUserService.current();
        List<LeaveRequest> leaves;
        if (currentUserService.isStaff(user)) {
            leaves = status == null
                    ? leaveRequestRepository.findAllByOrderByAppliedAtDesc()
                    : leaveRequestRepository.findByStatusOrderByAppliedAtDesc(status);
        } else {
            Long employeeId = currentUserService.requireEmployeeId(user);
            leaves = leaveRequestRepository.findByEmployeeId(employeeId).stream()
                    .filter(leave -> status == null || leave.getStatus() == status)
                    .sorted((a, b) -> b.getAppliedAt().compareTo(a.getAppliedAt()))
                    .toList();
        }
        return leaves.stream().map(LeaveResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<LeaveResponse> create(@Valid @RequestBody LeaveCreateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date cannot be before the start date");
        }
        User user = currentUserService.current();
        Long employeeId = currentUserService.isStaff(user)
                ? request.employeeId()
                : currentUserService.requireEmployeeId(user);
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee is required");
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        if (!currentUserService.isStaff(user)) {
            long requested = java.time.temporal.ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
            LeaveBalanceResponse balance = leaveBalanceService.balance(employeeId, request.startDate().getYear());
            if (requested > balance.remaining()) {
                throw new IllegalArgumentException("This request (" + requested + " day"
                        + (requested == 1 ? "" : "s") + ") exceeds your remaining leave balance of "
                        + balance.remaining() + " day" + (balance.remaining() == 1 ? "" : "s")
                        + " for " + balance.year() + ".");
            }
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setStartDate(request.startDate());
        leave.setEndDate(request.endDate());
        leave.setReason(request.reason().trim());
        leave.setStatus(LeaveStatus.PENDING);
        leave.setAppliedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRequestRepository.save(leave);

        if (!currentUserService.isStaff(user)) {
            notificationService.notifyRoles(List.of("ADMIN", "HR"), "Leave request",
                    employee.getFullName() + " requested leave (" + request.startDate() + " → " + request.endDate() + ")",
                    "LEAVE");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(LeaveResponse.from(saved));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public LeaveResponse updateStatus(@PathVariable Long id, @Valid @RequestBody LeaveStatusRequest request) {
        LeaveRequest leave = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leave request not found"));
        leave.setStatus(request.status());
        LeaveRequest saved = leaveRequestRepository.save(leave);
        notificationService.notifyEmployee(saved.getEmployee().getId(),
                "Leave " + request.status().name().toLowerCase(),
                "Your leave (" + saved.getStartDate() + " → " + saved.getEndDate() + ") was "
                        + request.status().name().toLowerCase(),
                "LEAVE");
        return LeaveResponse.from(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        LeaveRequest leave = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leave request not found"));
        leaveRequestRepository.delete(leave);
        return ResponseEntity.ok(Map.of("message", "Leave request deleted"));
    }
}
