package com.staffhub.repository;

import com.staffhub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByEmployeeId(Long employeeId);

    List<User> findByRoleInOrderByIdAsc(List<String> roles);

    List<User> findByRole(String role);
}
