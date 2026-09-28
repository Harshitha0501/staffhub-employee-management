package com.staffhub.dto;

import com.staffhub.model.Attendance;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** One day of attendance: the date actually used (server "today" when no date was passed), summary counts and rows. */
public record AttendanceDayResponse(
        LocalDate date,
        long presentCount,
        long checkedOutCount,
        List<AttendanceResponse> records) {

    public record AttendanceResponse(
            Long id,
            Long employeeId,
            String employeeName,
            String departmentName,
            LocalDate date,
            LocalTime checkIn,
            LocalTime checkOut) {

        public static AttendanceResponse from(Attendance attendance) {
            return new AttendanceResponse(
                    attendance.getId(),
                    attendance.getEmployee().getId(),
                    attendance.getEmployee().getFullName(),
                    attendance.getEmployee().getDepartment().getName(),
                    attendance.getWorkDate(),
                    attendance.getCheckIn(),
                    attendance.getCheckOut());
        }
    }
}
