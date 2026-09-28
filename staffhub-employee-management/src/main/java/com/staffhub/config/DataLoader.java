package com.staffhub.config;

import com.staffhub.model.Announcement;
import com.staffhub.model.Attendance;
import com.staffhub.model.Department;
import com.staffhub.model.Employee;
import com.staffhub.model.EmployeeStatus;
import com.staffhub.model.LeaveRequest;
import com.staffhub.model.LeaveStatus;
import com.staffhub.model.Notification;
import com.staffhub.model.Payroll;
import com.staffhub.model.PayrollStatus;
import com.staffhub.model.Task;
import com.staffhub.model.TaskPriority;
import com.staffhub.model.TaskStatus;
import com.staffhub.model.User;
import com.staffhub.repository.AnnouncementRepository;
import com.staffhub.repository.AttendanceRepository;
import com.staffhub.repository.DepartmentRepository;
import com.staffhub.repository.EmployeeRepository;
import com.staffhub.repository.LeaveRequestRepository;
import com.staffhub.repository.NotificationRepository;
import com.staffhub.repository.PayrollRepository;
import com.staffhub.repository.TaskRepository;
import com.staffhub.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Idempotent demo seed. Accounts:
 *   admin / admin123          (ADMIN)
 *   hr    / hr12345           (HR)
 *   {first}.{last} / welcome123  (EMPLOYEE, one per seeded employee)
 * Plus departments, employees, attendance, leave, payroll, tasks, announcements.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String EMPLOYEE_DEMO_PASSWORD = "welcome123";

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;
    private final TaskRepository taskRepository;
    private final AnnouncementRepository announcementRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository,
                      DepartmentRepository departmentRepository,
                      EmployeeRepository employeeRepository,
                      AttendanceRepository attendanceRepository,
                      LeaveRequestRepository leaveRequestRepository,
                      PayrollRepository payrollRepository,
                      TaskRepository taskRepository,
                      AnnouncementRepository announcementRepository,
                      NotificationRepository notificationRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.payrollRepository = payrollRepository;
        this.taskRepository = taskRepository;
        this.announcementRepository = announcementRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedStaffAccounts();

        if (departmentRepository.count() == 0) {
            departmentRepository.save(new Department("Engineering", "Builds and operates the product platform."));
            departmentRepository.save(new Department("Human Resources", "People, culture and workplace operations."));
            departmentRepository.save(new Department("Sales", "Revenue, accounts and client partnerships."));
            departmentRepository.save(new Department("Marketing", "Brand, demand generation and content."));
            departmentRepository.save(new Department("Finance", "Budgeting, accounting and payroll operations."));
        }

        if (employeeRepository.count() == 0) {
            seedEmployees();
            seedEmployeeAccounts();
            seedAttendance();
            seedLeaves();
            seedPreviousMonthPayroll();
        }

        if (taskRepository.count() == 0) {
            seedTasks();
        }
        if (announcementRepository.count() == 0) {
            seedAnnouncements();
        }
        if (notificationRepository.count() == 0) {
            seedNotifications();
        }
    }

    private void seedStaffAccounts() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User("admin", passwordEncoder.encode("admin123"), "Harshitha", "ADMIN");
            admin.setEmail("harshithaharshu0501@gmail.com");
            userRepository.save(admin);
        }
        if (userRepository.findByUsername("hr").isEmpty()) {
            User hr = new User("hr", passwordEncoder.encode("hr12345"), "Emma Fischer", "HR");
            hr.setEmail("emma.fischer@staffhub.io");
            userRepository.save(hr);
        }
    }

    private void seedEmployees() {
        String[][] rows = {
                {"Arjun", "Mehta", "Engineering", "Senior Software Engineer", "9800", "2019-03-11", "ACTIVE"},
                {"Sofia", "Ramirez", "Engineering", "Software Engineer", "7600", "2021-07-19", "ACTIVE"},
                {"Daniel", "Okafor", "Engineering", "QA Engineer", "6200", "2022-11-02", "ACTIVE"},
                {"Priya", "Nair", "Engineering", "Engineering Manager", "12400", "2018-06-04", "ACTIVE"},
                {"Emma", "Fischer", "Human Resources", "HR Manager", "8900", "2019-09-23", "ACTIVE"},
                {"Liam", "OConnor", "Human Resources", "HR Generalist", "5400", "2023-02-13", "ACTIVE"},
                {"Grace", "Wanjiru", "Sales", "Sales Executive", "7000", "2020-10-05", "ACTIVE"},
                {"Noah", "Kim", "Sales", "Account Manager", "6600", "2022-01-17", "ACTIVE"},
                {"Aisha", "Rahman", "Marketing", "Marketing Specialist", "5900", "2021-04-26", "ACTIVE"},
                {"Tomas", "Ferreira", "Marketing", "Content Strategist", "5300", "2023-08-14", "INACTIVE"},
                {"Elena", "Petrova", "Finance", "Financial Analyst", "7100", "2020-12-07", "ACTIVE"},
                {"Marcus", "Hale", "Finance", "Finance Manager", "10800", "2017-05-15", "ACTIVE"}
        };

        for (String[] row : rows) {
            Department department = departmentRepository.findByName(row[2])
                    .orElseThrow(() -> new IllegalStateException("Seed department missing: " + row[2]));
            Employee employee = new Employee();
            employee.setFirstName(row[0]);
            employee.setLastName(row[1]);
            employee.setEmail((row[0] + "." + row[1]).toLowerCase() + "@staffhub.io");
            employee.setPhone("+1 202 555 01" + String.format("%02d", employeeRepository.count() + 10));
            employee.setDepartment(department);
            employee.setRole(row[3]);
            employee.setSalary(new BigDecimal(row[4]));
            employee.setHireDate(LocalDate.parse(row[5]));
            employee.setStatus(EmployeeStatus.valueOf(row[6]));

            Employee saved = employeeRepository.save(employee);
            saved.setEmployeeCode(String.format("EMP-%04d", saved.getId()));
            employeeRepository.save(saved);
        }
    }

    /** One EMPLOYEE login per seeded employee: username first.last, password welcome123. */
    private void seedEmployeeAccounts() {
        for (Employee employee : employeeRepository.findAll()) {
            String username = (employee.getFirstName() + "." + employee.getLastName()).toLowerCase();
            if (userRepository.existsByUsername(username)) {
                continue;
            }
            User user = new User(username, passwordEncoder.encode(EMPLOYEE_DEMO_PASSWORD),
                    employee.getFullName(), "EMPLOYEE");
            user.setEmail(employee.getEmail());
            user.setEmployee(employee);
            userRepository.save(user);
        }
    }

    private void seedAttendance() {
        LocalDate today = LocalDate.now();
        seedAttendanceRow("Arjun", today, LocalTime.of(9, 12), null);
        seedAttendanceRow("Emma", today, LocalTime.of(8, 47), LocalTime.of(17, 31));
    }

    private void seedAttendanceRow(String firstName, LocalDate date, LocalTime in, LocalTime out) {
        Employee employee = employeeRepository.findByFirstName(firstName).stream().findFirst().orElse(null);
        if (employee == null) {
            return;
        }
        Attendance attendance = new Attendance(employee, date, in);
        attendance.setCheckOut(out);
        attendanceRepository.save(attendance);
    }

    private void seedLeaves() {
        LocalDate today = LocalDate.now();
        seedLeave("Sofia", today.plusDays(3), today.plusDays(5), "Family wedding out of state", LeaveStatus.PENDING, 1);
        seedLeave("Noah", today.plusDays(10), today.plusDays(12), "Short vacation with family", LeaveStatus.PENDING, 2);
        seedLeave("Elena", today.minusDays(7), today.minusDays(3), "Medical leave", LeaveStatus.APPROVED, 9);
    }

    private void seedLeave(String firstName, LocalDate start, LocalDate end, String reason,
                           LeaveStatus status, long appliedDaysAgo) {
        Employee employee = employeeRepository.findByFirstName(firstName).stream().findFirst().orElse(null);
        if (employee == null) {
            return;
        }
        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setReason(reason);
        leave.setStatus(status);
        leave.setAppliedAt(LocalDateTime.now().minusDays(appliedDaysAgo));
        leaveRequestRepository.save(leave);
    }

    private void seedPreviousMonthPayroll() {
        String lastMonth = YearMonth.now().minusMonths(1).format(MONTH_FORMAT);
        seedPayroll("Arjun", lastMonth, PayrollStatus.PAID);
        seedPayroll("Priya", lastMonth, PayrollStatus.PAID);
        seedPayroll("Elena", lastMonth, PayrollStatus.PENDING);
    }

    private void seedPayroll(String firstName, String month, PayrollStatus status) {
        Employee employee = employeeRepository.findByFirstName(firstName).stream().findFirst().orElse(null);
        if (employee == null) {
            return;
        }
        Payroll payroll = new Payroll(employee, month);
        payroll.setStatus(status);
        payrollRepository.save(payroll);
    }

    private void seedTasks() {
        LocalDate today = LocalDate.now();
        seedTask("Arjun", "Finalize Q3 sprint board", "Groom the backlog and lock scope for the next sprint.",
                TaskPriority.HIGH, TaskStatus.TODO, today.plusDays(4), "Emma Fischer", false);
        seedTask("Arjun", "Submit weekly timesheet", "Log hours for the current week before Friday.",
                TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, today.plusDays(1), "Emma Fischer", false);
        seedTask("Arjun", "Read the new security policy", "Personal reminder to review the updated policy doc.",
                TaskPriority.LOW, TaskStatus.TODO, today.plusDays(7), "Arjun Mehta", true);
        seedTask("Sofia", "Prepare release notes", "Draft notes for the 2.1 release.",
                TaskPriority.MEDIUM, TaskStatus.TODO, today.plusDays(3), "Emma Fischer", false);
        seedTask("Daniel", "Regression test payroll module", "Full pass before month-end payroll run.",
                TaskPriority.HIGH, TaskStatus.DONE, today.minusDays(1), "Emma Fischer", false);
    }

    private void seedTask(String firstName, String title, String description, TaskPriority priority,
                          TaskStatus status, LocalDate dueDate, String createdBy, boolean selfCreated) {
        Employee employee = employeeRepository.findByFirstName(firstName).stream().findFirst().orElse(null);
        if (employee == null) {
            return;
        }
        Task task = new Task();
        task.setEmployee(employee);
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setStatus(status);
        task.setDueDate(dueDate);
        task.setCreatedByName(createdBy);
        task.setSelfCreated(selfCreated);
        taskRepository.save(task);
    }

    private void seedAnnouncements() {
        Announcement welcome = new Announcement(
                "Welcome to StaffHub 2.0",
                "StaffHub now supports employee self-service. Log in to view your tasks, apply for leave, "
                        + "check in for the day and download your payslips. Reach out to HR with any questions.",
                "Harshitha");
        welcome.setCreatedAt(LocalDateTime.now().minusDays(2));
        announcementRepository.save(welcome);

        Announcement townhall = new Announcement(
                "Quarterly town hall — Friday 4 PM",
                "Join the all-hands in the main hall or via the video link. We will cover Q2 results and the roadmap.",
                "Emma Fischer");
        townhall.setCreatedAt(LocalDateTime.now().minusHours(6));
        announcementRepository.save(townhall);
    }

    private void seedNotifications() {
        notificationRepository.save(withTime(new Notification("arjun.mehta", "New task assigned",
                "Emma Fischer assigned you: Finalize Q3 sprint board", "TASK"), LocalDateTime.now().minusHours(3)));
        notificationRepository.save(withTime(new Notification("arjun.mehta", "New announcement",
                "Welcome to StaffHub 2.0", "ANNOUNCEMENT"), LocalDateTime.now().minusDays(2)));
    }

    private Notification withTime(Notification notification, LocalDateTime time) {
        notification.setCreatedAt(time);
        return notification;
    }
}
