package com.group9.topicmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Immutable mail payloads are dispatched only after a successful database commit. */
@Service
public class TransactionalNotificationService {
    private static final Logger log = LoggerFactory.getLogger(TransactionalNotificationService.class);
    public record Notification(String recipient, String subject, String body) {}
    private final ApplicationEventPublisher events;
    private final EmailService emailService;

    public TransactionalNotificationService(ApplicationEventPublisher events, EmailService emailService) {
        this.events = events;
        this.emailService = emailService;
    }

    public void sendAfterCommit(String recipient, String subject, String body) {
        events.publishEvent(new Notification(recipient, subject, body));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(Notification notification) {
        try {
            emailService.sendNotification(notification.recipient(), notification.subject(), notification.body());
        } catch (RuntimeException failure) {
            // The database is committed. A full async queue must not turn success into HTTP 500.
            log.warn("EMAIL dispatch failed after commit: {}", failure.getClass().getSimpleName());
        }
    }
}
