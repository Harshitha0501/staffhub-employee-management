package com.staffhub.web;

import com.staffhub.dto.AttendanceDayResponse;
import com.staffhub.dto.CheckRequest;
import com.staffhub.model.Attendance;
import com.staffhub.model.User;
import com.staffhub.repository.AttendanceRepository;
import com.staffhub.service.AttendanceService;
import com.staffhub.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceRepository attendanceRepository;
    private final CurrentUserService currentUserService;

    public AttendanceController(AttendanceService attendanceService,
                                AttendanceRepository attendanceRepository,
                                CurrentUserService currentUserService) {
        this.attendanceService = attendanceService;
        this.attendanceRepository = attendanceRepository;
        this.currentUserService = currentUserService;
    }

    /** Company-wide day log; staff only. No date param defaults to the server's today. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public AttendanceDayResponse day(@RequestParam(required = false) LocalDate date) {
        return attendanceService.day(date == null ? LocalDate.now() : date);
    }

    /** The signed-in employee's own attendance: today's record plus recent history. */
    @GetMapping("/mine")
    public Map<String, Object> mine() {
        User user = currentUserService.current();
        Long employeeId = currentUserService.requireEmployeeId(user);
        LocalDate today = LocalDate.now();
        Attendance todayRecord = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today).orElse(null);
        List<AttendanceDayResponse.AttendanceResponse> history = attendanceRepository
                .findByEmployeeIdOrderByWorkDateDesc(employeeId).stream()
                .map(AttendanceDayResponse.AttendanceResponse::from)
                .toList();
        Map<String, Object> result = new HashMap<>();
        result.put("todayIso", today.toString());
        result.put("today", todayRecord == null ? null : AttendanceDayResponse.AttendanceResponse.from(todayRecord));
        result.put("records", history);
        return result;
    }

    @PostMapping("/check-in")
    public AttendanceDayResponse.AttendanceResponse checkIn(@RequestBody(required = false) CheckRequest request) {
        return AttendanceDayResponse.AttendanceResponse.from(attendanceService.checkIn(resolveEmployeeId(request)));
    }

    @PostMapping("/check-out")
    public AttendanceDayResponse.AttendanceResponse checkOut(@RequestBody(required = false) CheckRequest request) {
        return AttendanceDayResponse.AttendanceResponse.from(attendanceService.checkOut(resolveEmployeeId(request)));
    }

    /** Employees always act on their own record; staff may pass an employeeId. */
    private Long resolveEmployeeId(CheckRequest request) {
        User user = currentUserService.current();
        if (currentUserService.isStaff(user)) {
            if (request == null || request.employeeId() == null) {
                throw new IllegalArgumentException("Employee is required");
            }
            return request.employeeId();
        }
        return currentUserService.requireEmployeeId(user);
    }
}
