package com.staffhub.service;

import com.staffhub.exception.NotFoundException;
import com.staffhub.model.User;
import com.staffhub.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Resolves the authenticated {@link User} from the Spring Security context. */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new NotFoundException("Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole());
    }

    public boolean isHr(User user) {
        return "HR".equals(user.getRole());
    }

    /** ADMIN or HR — the "staff" who manage other people's records. */
    public boolean isStaff(User user) {
        return isAdmin(user) || isHr(user);
    }

    /** The employee id tied to an EMPLOYEE account (or null for admin/HR). */
    public Long employeeId(User user) {
        return user.getEmployee() == null ? null : user.getEmployee().getId();
    }

    public Long requireEmployeeId(User user) {
        Long id = employeeId(user);
        if (id == null) {
            throw new IllegalStateException("This account is not linked to an employee record");
        }
        return id;
    }
}
