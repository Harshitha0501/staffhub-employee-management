package com.staffhub.repository;

import com.staffhub.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername);

    long countByRecipientUsernameAndReadFalse(String recipientUsername);

    List<Notification> findByRecipientUsernameAndReadFalse(String recipientUsername);
}
