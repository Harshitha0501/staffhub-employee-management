package com.staffhub.service;

import com.staffhub.dto.EmployeeRequest;
import com.staffhub.exception.NotFoundException;
import com.staffhub.model.Department;
import com.staffhub.model.Employee;
import com.staffhub.model.EmployeeStatus;
import com.staffhub.model.LeaveRequest;
import com.staffhub.model.Payroll;
import com.staffhub.model.Attendance;
import com.staffhub.model.User;
import com.staffhub.repository.AttendanceRepository;
import com.staffhub.repository.DepartmentRepository;
import com.staffhub.repository.EmployeeRepository;
import com.staffhub.repository.LeaveRequestRepository;
import com.staffhub.repository.PayrollRepository;
import com.staffhub.repository.TaskRepository;
import com.staffhub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Employee business logic: search/filter, employee-code generation,
 * email uniqueness and cascade delete of attendance/leaves/payrolls.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           AttendanceRepository attendanceRepository,
                           LeaveRequestRepository leaveRequestRepository,
                           PayrollRepository payrollRepository,
                           TaskRepository taskRepository,
                           UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.payrollRepository = payrollRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /**
     * Search + filter. Demo scale is small, so filtering happens in memory —
     * swap for a JPA Specification or paginated query when the table grows.
     */
    public List<Employee> search(String q, Long departmentId, EmployeeStatus status) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return employeeRepository.findAll().stream()
                .filter(employee -> needle.isEmpty()
                        || employee.getFullName().toLowerCase().contains(needle)
                        || employee.getEmail().toLowerCase().contains(needle)
                        || employee.getEmployeeCode().toLowerCase().contains(needle)
                        || employee.getRole().toLowerCase().contains(needle))
                .filter(employee -> departmentId == null || departmentId == 0
                        || employee.getDepartment().getId().equals(departmentId))
                .filter(employee -> status == null || employee.getStatus() == status)
                .sorted(Comparator.comparing(Employee::getId).reversed())
                .toList();
    }

    public Employee get(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    @Transactional
    public Employee create(EmployeeRequest request) {
        String email = request.email().trim().toLowerCase();
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An employee with this email already exists");
        }
        Department department = findDepartment(request.departmentId());

        Employee employee = new Employee();
        applyRequest(employee, request, email, department);
        employee.setStatus(request.status() == null ? EmployeeStatus.ACTIVE : request.status());

        Employee saved = employeeRepository.save(employee);
        saved.setEmployeeCode(String.format("EMP-%04d", saved.getId()));
        return employeeRepository.save(saved);
    }

    @Transactional
    public Employee update(Long id, EmployeeRequest request) {
        Employee employee = get(id);
        String email = request.email().trim().toLowerCase();
        if (!employee.getEmail().equalsIgnoreCase(email)
                && employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Another employee already uses this email");
        }
        Department department = findDepartment(request.departmentId());
        applyRequest(employee, request, email, department);
        if (request.status() != null) {
            employee.setStatus(request.status());
        }
        return employeeRepository.save(employee);
    }

    /** Deletes the employee together with their attendance, leave requests and payslips. */
    @Transactional
    public void delete(Long id) {
        Employee employee = get(id);
        attendanceRepository.deleteAll(attendanceRepository.findByEmployeeId(id));
        leaveRequestRepository.deleteAll(leaveRequestRepository.findByEmployeeId(id));
        payrollRepository.deleteAll(payrollRepository.findByEmployeeId(id));
        taskRepository.deleteAll(taskRepository.findByEmployeeId(id));
        User account = userRepository.findByEmployeeId(id).orElse(null);
        if (account != null) {
            userRepository.delete(account);
        }
        employeeRepository.delete(employee);
    }

    private void applyRequest(Employee employee, EmployeeRequest request, String email, Department department) {
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(email);
        employee.setPhone(request.phone() == null ? null : request.phone().trim());
        employee.setDepartment(department);
        employee.setRole(request.role().trim());
        employee.setSalary(request.salary());
        employee.setHireDate(request.hireDate());
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new NotFoundException("Department not found"));
    }
}
