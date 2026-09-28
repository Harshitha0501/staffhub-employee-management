package com.staffhub.service;

import com.staffhub.model.Employee;
import com.staffhub.model.User;
import com.staffhub.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Provisions login accounts: generates unique usernames and secure random
 * passwords, hashes them with BCrypt, and returns the raw password ONCE.
 */
@Service
public class ProvisioningService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProvisioningService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record Provisioned(User user, String rawPassword) {
    }

    /**
     * Creates a login account. When {@code desiredUsername} / {@code rawPassword}
     * are blank they are auto-generated.
     */
    public Provisioned createUser(String fullName, String email, String role, Employee employee,
                                  String desiredUsername, String rawPassword) {
        String username = (desiredUsername == null || desiredUsername.isBlank())
                ? uniqueUsername(fullName)
                : uniqueExact(desiredUsername.trim().toLowerCase());
        String password = (rawPassword == null || rawPassword.isBlank())
                ? randomPassword(10)
                : rawPassword;

        User user = new User(username, passwordEncoder.encode(password), fullName.trim(), role);
        user.setEmail(email == null || email.isBlank() ? null : email.trim().toLowerCase());
        user.setEmployee(employee);
        user.setEnabled(true);
        userRepository.save(user);
        return new Provisioned(user, password);
    }

    public String resetPassword(User user) {
        String password = randomPassword(10);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return password;
    }

    public String uniqueUsername(String fullName) {
        String base = fullName.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", ".")
                .replaceAll("^\\.|\\.$", "");
        if (base.isBlank()) {
            base = "user";
        }
        return uniqueExact(base);
    }

    private String uniqueExact(String base) {
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }

    public String randomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET[RNG.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
