package com.staffhub.web;

import com.staffhub.dto.NotificationResponse;
import com.staffhub.exception.NotFoundException;
import com.staffhub.model.Notification;
import com.staffhub.model.User;
import com.staffhub.repository.NotificationRepository;
import com.staffhub.service.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Per-user in-app notifications. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationRepository notificationRepository,
                                  CurrentUserService currentUserService) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Map<String, Object> list() {
        User user = currentUserService.current();
        List<NotificationResponse> items = notificationRepository
                .findByRecipientUsernameOrderByCreatedAtDesc(user.getUsername()).stream()
                .map(NotificationResponse::from)
                .toList();
        long unread = notificationRepository.countByRecipientUsernameAndReadFalse(user.getUsername());
        return Map.of("unread", unread, "items", items);
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        User user = currentUserService.current();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (!notification.getRecipientUsername().equals(user.getUsername())) {
            throw new NotFoundException("Notification not found");
        }
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @PutMapping("/read-all")
    public Map<String, String> markAllRead() {
        User user = currentUserService.current();
        List<Notification> unread = notificationRepository.findByRecipientUsernameAndReadFalse(user.getUsername());
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
        return Map.of("message", "All caught up");
    }
}
