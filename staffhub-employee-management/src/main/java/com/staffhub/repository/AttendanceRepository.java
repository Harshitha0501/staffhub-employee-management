package com.staffhub.repository;

import com.staffhub.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    List<Attendance> findByWorkDateOrderByCheckInAsc(LocalDate workDate);

    long countByWorkDate(LocalDate workDate);

    List<Attendance> findByEmployeeId(Long employeeId);

    List<Attendance> findByEmployeeIdOrderByWorkDateDesc(Long employeeId);
}
