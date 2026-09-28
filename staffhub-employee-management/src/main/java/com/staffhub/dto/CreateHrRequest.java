package com.staffhub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/users/hr. Username and password are optional (auto-generated when blank). */
public record CreateHrRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @Email(message = "Email must be valid") String email,
        String username,
        String password) {
}
