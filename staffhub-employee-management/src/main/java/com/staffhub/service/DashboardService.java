package com.staffhub.service;

import com.staffhub.dto.AnnouncementResponse;
import com.staffhub.dto.DashboardStats;
import com.staffhub.dto.EmployeeDashboardResponse;
import com.staffhub.dto.EmployeeResponse;
import com.staffhub.dto.LeaveBalanceResponse;
import com.staffhub.dto.TaskResponse;
import com.staffhub.model.Attendance;
import com.staffhub.model.Employee;
import com.staffhub.model.EmployeeStatus;
import com.staffhub.model.LeaveRequest;
import com.staffhub.model.LeaveStatus;
import com.staffhub.model.TaskStatus;
import com.staffhub.repository.AnnouncementRepository;
import com.staffhub.repository.AttendanceRepository;
import com.staffhub.repository.DepartmentRepository;
import com.staffhub.repository.EmployeeRepository;
import com.staffhub.repository.LeaveRequestRepository;
import com.staffhub.repository.PayrollRepository;
import com.staffhub.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DashboardService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;
    private final TaskRepository taskRepository;
    private final AnnouncementRepository announcementRepository;
    private final LeaveBalanceService leaveBalanceService;

    public DashboardService(EmployeeRepository employeeRepository,
                            DepartmentRepository departmentRepository,
                            AttendanceRepository attendanceRepository,
                            LeaveRequestRepository leaveRequestRepository,
                            PayrollRepository payrollRepository,
                            TaskRepository taskRepository,
                            AnnouncementRepository announcementRepository,
                            LeaveBalanceService leaveBalanceService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.payrollRepository = payrollRepository;
        this.taskRepository = taskRepository;
        this.announcementRepository = announcementRepository;
        this.leaveBalanceService = leaveBalanceService;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        long totalEmployees = employeeRepository.count();
        long activeEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long presentToday = attendanceRepository.countByWorkDate(LocalDate.now());
        long pendingLeaves = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);

        String currentMonth = YearMonth.now().format(MONTH_FORMAT);
        BigDecimal monthlyPayroll = payrollRepository.findByPayMonthOrderByEmployeeIdAsc(currentMonth)
                .stream()
                .map(payroll -> payroll.getNetPay() == null ? BigDecimal.ZERO : payroll.getNetPay())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<EmployeeResponse> recentHires = employeeRepository.findTop5ByOrderByIdDesc()
                .stream()
                .map(EmployeeResponse::from)
                .toList();

        return new DashboardStats(totalEmployees, activeEmployees, totalEmployees - activeEmployees,
                departmentRepository.count(), presentToday, pendingLeaves, monthlyPayroll,
                currentMonth, recentHires);
    }

    @Transactional(readOnly = true)
    public EmployeeDashboardResponse employeeDashboard(Employee employee) {
        Long id = employee.getId();

        long openTasks = taskRepository.countByEmployeeIdAndStatus(id, TaskStatus.TODO)
                + taskRepository.countByEmployeeIdAndStatus(id, TaskStatus.IN_PROGRESS);
        long doneTasks = taskRepository.countByEmployeeIdAndStatus(id, TaskStatus.DONE);

        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeId(id);
        long pendingLeaves = leaves.stream().filter(l -> l.getStatus() == LeaveStatus.PENDING).count();
        long approvedLeaves = leaves.stream().filter(l -> l.getStatus() == LeaveStatus.APPROVED).count();

        Attendance todayRecord = attendanceRepository
                .findByEmployeeIdAndWorkDate(id, LocalDate.now()).orElse(null);
        boolean checkedIn = todayRecord != null;
        String checkInTime = todayRecord == null || todayRecord.getCheckIn() == null
                ? null : todayRecord.getCheckIn().toString();
        String checkOutTime = todayRecord == null || todayRecord.getCheckOut() == null
                ? null : todayRecord.getCheckOut().toString();

        List<TaskResponse> recentTasks = taskRepository.findByEmployeeIdOrderByCreatedAtDesc(id).stream()
                .limit(5)
                .map(TaskResponse::from)
                .toList();
        List<AnnouncementResponse> recentAnnouncements = announcementRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(3)
                .map(AnnouncementResponse::from)
                .toList();

        LeaveBalanceResponse leaveBalance = leaveBalanceService.balance(id, java.time.Year.now().getValue());

        return new EmployeeDashboardResponse(
                employee.getFullName(),
                employee.getDepartment().getName(),
                employee.getRole(),
                openTasks,
                doneTasks,
                pendingLeaves,
                approvedLeaves,
                checkedIn,
                checkInTime,
                checkOutTime,
                recentTasks,
                recentAnnouncements,
                leaveBalance.quota(),
                leaveBalance.remaining());
    }
}
