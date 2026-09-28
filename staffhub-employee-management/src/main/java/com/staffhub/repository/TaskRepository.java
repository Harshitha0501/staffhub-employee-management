package com.staffhub.repository;

import com.staffhub.model.Task;
import com.staffhub.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByOrderByCreatedAtDesc();

    List<Task> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<Task> findByEmployeeId(Long employeeId);

    long countByEmployeeIdAndStatus(Long employeeId, TaskStatus status);

    long countByStatus(TaskStatus status);
}
