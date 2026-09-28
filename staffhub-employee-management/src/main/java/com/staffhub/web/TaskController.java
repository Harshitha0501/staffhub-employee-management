package com.staffhub.web;

import com.staffhub.dto.TaskRequest;
import com.staffhub.dto.TaskResponse;
import com.staffhub.dto.TaskStatusRequest;
import com.staffhub.exception.NotFoundException;
import com.staffhub.model.Employee;
import com.staffhub.model.Task;
import com.staffhub.model.TaskPriority;
import com.staffhub.model.TaskStatus;
import com.staffhub.model.User;
import com.staffhub.repository.EmployeeRepository;
import com.staffhub.repository.TaskRepository;
import com.staffhub.service.CurrentUserService;
import com.staffhub.service.NotificationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Tasks. ADMIN/HR assign tasks to any employee; employees create their own and
 * update the status of tasks that belong to them.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public TaskController(TaskRepository taskRepository,
                          EmployeeRepository employeeRepository,
                          CurrentUserService currentUserService,
                          NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<TaskResponse> list(@RequestParam(required = false) Long employeeId,
                                   @RequestParam(required = false) TaskStatus status) {
        User user = currentUserService.current();
        List<Task> tasks;
        if (currentUserService.isStaff(user)) {
            tasks = employeeId != null
                    ? taskRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                    : taskRepository.findAllByOrderByCreatedAtDesc();
        } else {
            tasks = taskRepository.findByEmployeeIdOrderByCreatedAtDesc(currentUserService.requireEmployeeId(user));
        }
        return tasks.stream()
                .filter(task -> status == null || task.getStatus() == status)
                .map(TaskResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        User user = currentUserService.current();
        boolean staff = currentUserService.isStaff(user);

        Long targetEmployeeId = staff
                ? (request.employeeId() != null ? request.employeeId() : currentUserService.employeeId(user))
                : currentUserService.requireEmployeeId(user);
        if (targetEmployeeId == null) {
            throw new IllegalArgumentException("Pick an employee to assign this task to");
        }
        Employee employee = employeeRepository.findById(targetEmployeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        Task task = new Task();
        task.setEmployee(employee);
        task.setTitle(request.title().trim());
        task.setDescription(request.description() == null ? null : request.description().trim());
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setDueDate(request.dueDate());
        task.setStatus(TaskStatus.TODO);
        task.setCreatedByName(user.getFullName());
        task.setSelfCreated(!staff || targetEmployeeId.equals(currentUserService.employeeId(user)));

        Task saved = taskRepository.save(task);

        if (staff && !saved.isSelfCreated()) {
            notificationService.notifyEmployee(employee.getId(), "New task assigned",
                    user.getFullName() + " assigned you: " + saved.getTitle(), "TASK");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(saved));
    }

    @PutMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusRequest request) {
        Task task = requireAccessible(id);
        task.setStatus(request.status());
        return TaskResponse.from(taskRepository.save(task));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        Task task = requireAccessible(id);
        taskRepository.delete(task);
        return ResponseEntity.ok(Map.of("message", "Task deleted"));
    }

    /** A task the current user may act on: staff on any, employee only on their own. */
    private Task requireAccessible(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        User user = currentUserService.current();
        if (!currentUserService.isStaff(user)) {
            Long myEmployeeId = currentUserService.requireEmployeeId(user);
            if (!task.getEmployee().getId().equals(myEmployeeId)) {
                throw new NotFoundException("Task not found");
            }
        }
        return task;
    }
}
