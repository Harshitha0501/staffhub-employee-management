package com.staffhub.service;

import com.staffhub.dto.AttendanceDayResponse;
import com.staffhub.exception.NotFoundException;
import com.staffhub.model.Attendance;
import com.staffhub.model.Employee;
import com.staffhub.repository.AttendanceRepository;
import com.staffhub.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Daily attendance: one row per employee per day, check-in then check-out. */
@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    public AttendanceDayResponse day(LocalDate date) {
        List<Attendance> records = attendanceRepository.findByWorkDateOrderByCheckInAsc(date);
        long checkedOut = records.stream().filter(record -> record.getCheckOut() != null).count();
        List<AttendanceDayResponse.AttendanceResponse> rows = records.stream()
                .map(AttendanceDayResponse.AttendanceResponse::from)
                .toList();
        return new AttendanceDayResponse(date, records.size(), checkedOut, rows);
    }

    @Transactional
    public Attendance checkIn(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        LocalDate today = LocalDate.now();
        attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today).ifPresent(existing -> {
            throw new IllegalStateException(employee.getFullName() + " already checked in today");
        });
        return attendanceRepository.save(new Attendance(employee, today, LocalTime.now()));
    }

    @Transactional
    public Attendance checkOut(Long employeeId) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today)
                .orElseThrow(() -> new IllegalStateException("No check-in found for this employee today"));
        if (attendance.getCheckOut() != null) {
            throw new IllegalStateException(attendance.getEmployee().getFullName() + " already checked out today");
        }
        attendance.setCheckOut(LocalTime.now());
        return attendanceRepository.save(attendance);
    }
}
