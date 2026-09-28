package com.staffhub.web;

import com.staffhub.dto.EmployeeCreateResponse;
import com.staffhub.dto.EmployeeRequest;
import com.staffhub.dto.EmployeeResponse;
import com.staffhub.dto.ProvisionedResponse;
import com.staffhub.exception.NotFoundException;
import com.staffhub.model.Employee;
import com.staffhub.model.EmployeeStatus;
import com.staffhub.model.User;
import com.staffhub.repository.UserRepository;
import com.staffhub.service.CurrentUserService;
import com.staffhub.service.EmployeeService;
import com.staffhub.service.ProvisioningService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ProvisioningService provisioningService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public EmployeeController(EmployeeService employeeService,
                             ProvisioningService provisioningService,
                             UserRepository userRepository,
                             CurrentUserService currentUserService) {
        this.employeeService = employeeService;
        this.provisioningService = provisioningService;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    /** List / search / filter. Params: q, departmentId, status (ACTIVE|INACTIVE). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public List<EmployeeResponse> list(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) Long departmentId,
                                       @RequestParam(required = false) EmployeeStatus status) {
        return employeeService.search(q, departmentId, status).stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    /** Predefined job titles for the role datalist in the employee form. */
    @GetMapping("/roles")
    public List<String> roles() {
        return List.of(
                "Engineering Manager", "Senior Software Engineer", "Software Engineer", "QA Engineer",
                "Product Designer", "HR Manager", "HR Generalist", "Sales Executive", "Account Manager",
                "Marketing Specialist", "Content Strategist", "Financial Analyst", "Finance Manager",
                "Operations Manager");
    }

    /** The signed-in employee's own record. */
    @GetMapping("/me")
    public EmployeeResponse me() {
        User user = currentUserService.current();
        Long employeeId = currentUserService.requireEmployeeId(user);
        return EmployeeResponse.from(employeeService.get(employeeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EmployeeResponse get(@PathVariable Long id) {
        return EmployeeResponse.from(employeeService.get(id));
    }

    /** The linked login username for an employee (null if none). */
    @GetMapping("/{id}/account")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Map<String, Object> account(@PathVariable Long id) {
        User user = userRepository.findByEmployeeId(id).orElse(null);
        return Map.of(
                "hasAccount", user != null,
                "username", user == null ? "" : user.getUsername(),
                "enabled", user != null && user.isEnabled());
    }

    /** Create an employee AND auto-provision their EMPLOYEE login. Returns one-time credentials. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<EmployeeCreateResponse> create(@Valid @RequestBody EmployeeRequest request) {
        Employee employee = employeeService.create(request);
        ProvisioningService.Provisioned provisioned = provisioningService.createUser(
                employee.getFullName(), employee.getEmail(), "EMPLOYEE", employee,
                employee.getFirstName() + "." + employee.getLastName(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(new EmployeeCreateResponse(
                EmployeeResponse.from(employee),
                provisioned.user().getUsername(),
                provisioned.rawPassword()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return EmployeeResponse.from(employeeService.update(id, request));
    }

    /** Reset (or create, if missing) the employee's login and return the new password once. */
    @PostMapping("/{id}/reset-account")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ProvisionedResponse resetAccount(@PathVariable Long id) {
        Employee employee = employeeService.get(id);
        User user = userRepository.findByEmployeeId(id).orElse(null);
        if (user == null) {
            ProvisioningService.Provisioned provisioned = provisioningService.createUser(
                    employee.getFullName(), employee.getEmail(), "EMPLOYEE", employee,
                    employee.getFirstName() + "." + employee.getLastName(), null);
            return new ProvisionedResponse(provisioned.user().getUsername(), provisioned.rawPassword(),
                    employee.getFullName(), "EMPLOYEE", "Login created for " + employee.getFullName());
        }
        String password = provisioningService.resetPassword(user);
        return new ProvisionedResponse(user.getUsername(), password, employee.getFullName(), "EMPLOYEE",
                "New password generated for " + employee.getFullName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Employee deleted"));
    }
}
