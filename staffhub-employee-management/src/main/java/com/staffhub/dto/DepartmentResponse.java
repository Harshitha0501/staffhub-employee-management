package com.staffhub.dto;

import com.staffhub.model.Department;

public record DepartmentResponse(Long id, String name, String description, long employeeCount) {

    public static DepartmentResponse from(Department department, long employeeCount) {
        return new DepartmentResponse(department.getId(), department.getName(),
                department.getDescription(), employeeCount);
    }
}
