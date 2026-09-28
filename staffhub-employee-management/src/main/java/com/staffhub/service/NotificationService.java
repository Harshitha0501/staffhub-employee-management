package com.staffhub.service;

import com.staffhub.model.Notification;
import com.staffhub.model.User;
import com.staffhub.repository.NotificationRepository;
import com.staffhub.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Creates in-app notifications for users. */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void notifyUsername(String username, String title, String message, String type) {
        if (username == null) {
            return;
        }
        notificationRepository.save(new Notification(username, title, message, type));
    }

    /** Notify the login account tied to a given employee (if one exists). */
    public void notifyEmployee(Long employeeId, String title, String message, String type) {
        userRepository.findByEmployeeId(employeeId)
                .ifPresent(user -> notifyUsername(user.getUsername(), title, message, type));
    }

    /** Notify every user holding one of the given roles. */
    public void notifyRoles(List<String> roles, String title, String message, String type) {
        for (String role : roles) {
            for (User user : userRepository.findByRole(role)) {
                notifyUsername(user.getUsername(), title, message, type);
            }
        }
    }

    /** Notify all EMPLOYEE accounts (used for announcements). */
    public void notifyAllEmployees(String title, String message, String type) {
        for (User user : userRepository.findByRole("EMPLOYEE")) {
            notifyUsername(user.getUsername(), title, message, type);
        }
    }
}
