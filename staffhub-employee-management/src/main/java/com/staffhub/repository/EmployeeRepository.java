package com.staffhub.repository;

import com.staffhub.model.Employee;
import com.staffhub.model.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmailIgnoreCase(String email);

    long countByDepartmentId(Long departmentId);

    long countByStatus(EmployeeStatus status);

    List<Employee> findByStatus(EmployeeStatus status);

    List<Employee> findByFirstName(String firstName);

    List<Employee> findTop5ByOrderByIdDesc();
}
