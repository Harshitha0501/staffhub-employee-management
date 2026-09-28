package com.staffhub.web;

import com.staffhub.dto.AnnouncementRequest;
import com.staffhub.dto.AnnouncementResponse;
import com.staffhub.exception.NotFoundException;
import com.staffhub.model.Announcement;
import com.staffhub.model.User;
import com.staffhub.repository.AnnouncementRepository;
import com.staffhub.service.CurrentUserService;
import com.staffhub.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Company announcements: everyone can read, ADMIN/HR can post and delete. */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public AnnouncementController(AnnouncementRepository announcementRepository,
                                  CurrentUserService currentUserService,
                                  NotificationService notificationService) {
        this.announcementRepository = announcementRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<AnnouncementResponse> list() {
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AnnouncementResponse::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<AnnouncementResponse> create(@Valid @RequestBody AnnouncementRequest request) {
        User user = currentUserService.current();
        Announcement announcement = new Announcement(request.title().trim(), request.body().trim(), user.getFullName());
        Announcement saved = announcementRepository.save(announcement);
        notificationService.notifyAllEmployees("New announcement", saved.getTitle(), "ANNOUNCEMENT");
        return ResponseEntity.status(HttpStatus.CREATED).body(AnnouncementResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Announcement not found"));
        announcementRepository.delete(announcement);
        return ResponseEntity.ok(Map.of("message", "Announcement deleted"));
    }
}
