package com.staffhub.web;

import com.staffhub.dto.CreateHrRequest;
import com.staffhub.dto.ProvisionedResponse;
import com.staffhub.dto.UserResponse;
import com.staffhub.exception.NotFoundException;
import com.staffhub.model.User;
import com.staffhub.repository.UserRepository;
import com.staffhub.service.CurrentUserService;
import com.staffhub.service.ProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Account management. Admin provisions HR accounts and can reset / disable them. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final ProvisioningService provisioningService;
    private final CurrentUserService currentUserService;

    public UserController(UserRepository userRepository,
                          ProvisioningService provisioningService,
                          CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.provisioningService = provisioningService;
        this.currentUserService = currentUserService;
    }

    /** Admin console: list the ADMIN and HR staff accounts. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> staffAccounts() {
        return userRepository.findByRoleInOrderByIdAsc(List.of("ADMIN", "HR")).stream()
                .map(UserResponse::from)
                .toList();
    }

    @PostMapping("/hr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProvisionedResponse> createHr(@Valid @RequestBody CreateHrRequest request) {
        ProvisioningService.Provisioned provisioned = provisioningService.createUser(
                request.fullName(), request.email(), "HR", null, request.username(), request.password());
        User user = provisioned.user();
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProvisionedResponse(
                user.getUsername(), provisioned.rawPassword(), user.getFullName(), user.getRole(),
                "HR account created for " + user.getFullName()));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ProvisionedResponse resetPassword(@PathVariable Long id) {
        User user = find(id);
        if ("EMPLOYEE".equals(user.getRole())) {
            throw new IllegalArgumentException("Reset employee logins from the Employees screen");
        }
        String password = provisioningService.resetPassword(user);
        return new ProvisionedResponse(user.getUsername(), password, user.getFullName(), user.getRole(),
                "New password generated for " + user.getFullName());
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse toggle(@PathVariable Long id) {
        User user = find(id);
        if (user.getUsername().equals(currentUserService.current().getUsername())) {
            throw new IllegalArgumentException("You cannot disable your own account");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("Admin accounts cannot be disabled");
        }
        user.setEnabled(!user.isEnabled());
        return UserResponse.from(userRepository.save(user));
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Account not found"));
    }
}
