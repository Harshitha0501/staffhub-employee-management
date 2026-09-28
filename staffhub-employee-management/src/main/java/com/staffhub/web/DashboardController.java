package com.staffhub.web;

import com.staffhub.dto.DashboardStats;
import com.staffhub.dto.EmployeeDashboardResponse;
import com.staffhub.model.User;
import com.staffhub.service.CurrentUserService;
import com.staffhub.service.DashboardService;
import com.staffhub.service.EmployeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final EmployeeService employeeService;
    private final CurrentUserService currentUserService;

    public DashboardController(DashboardService dashboardService,
                              EmployeeService employeeService,
                              CurrentUserService currentUserService) {
        this.dashboardService = dashboardService;
        this.employeeService = employeeService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public DashboardStats stats() {
        return dashboardService.stats();
    }

    @GetMapping("/me")
    public EmployeeDashboardResponse me() {
        User user = currentUserService.current();
        Long employeeId = currentUserService.requireEmployeeId(user);
        return dashboardService.employeeDashboard(employeeService.get(employeeId));
    }
}
