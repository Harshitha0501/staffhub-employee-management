package com.staffhub.dto;

/** One-time credentials returned to the creator after provisioning / password reset. */
public record ProvisionedResponse(
        String username,
        String password,
        String fullName,
        String role,
        String message) {
}
