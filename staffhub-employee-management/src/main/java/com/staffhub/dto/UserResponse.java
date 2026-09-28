package com.staffhub.dto;

import com.staffhub.model.User;

/** Safe user shape for account-management screens (never exposes the password hash). */
public record UserResponse(
        Long id,
        String username,
        String fullName,
        String role,
        String email,
        boolean enabled,
        Long employeeId) {

    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getRole(),
                u.getEmail(),
                u.isEnabled(),
                u.getEmployee() == null ? null : u.getEmployee().getId());
    }
}
