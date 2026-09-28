package com.staffhub.web;

import com.staffhub.dto.DepartmentRequest;
import com.staffhub.dto.DepartmentResponse;
import com.staffhub.exception.NotFoundException;
import com.staffhub.model.Department;
import com.staffhub.repository.DepartmentRepository;
import com.staffhub.repository.EmployeeRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentController(DepartmentRepository departmentRepository,
                                EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public List<DepartmentResponse> list() {
        return departmentRepository.findAll().stream()
                .sorted(Comparator.comparing(Department::getId))
                .map(department -> DepartmentResponse.from(department,
                        employeeRepository.countByDepartmentId(department.getId())))
                .toList();
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A department with this name already exists");
        }
        Department department = new Department(name, trimDescription(request.description()));
        Department saved = departmentRepository.save(department);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DepartmentResponse.from(saved, 0));
    }

    @PutMapping("/{id}")
    public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found"));
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("A department with this name already exists");
        }
        department.setName(name);
        department.setDescription(trimDescription(request.description()));
        Department saved = departmentRepository.save(department);
        return DepartmentResponse.from(saved, employeeRepository.countByDepartmentId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found"));
        if (employeeRepository.countByDepartmentId(id) > 0) {
            throw new IllegalStateException("Move or remove the employees in this department before deleting it");
        }
        departmentRepository.delete(department);
        return ResponseEntity.ok(Map.of("message", "Department deleted"));
    }

    private String trimDescription(String description) {
        return description == null ? null : description.trim();
    }
}
