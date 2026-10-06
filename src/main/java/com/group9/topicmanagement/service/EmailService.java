package com.group9.topicmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

/** TV3 calls this service only after the business transaction has committed. */
@Service
public class EmailService {
    public enum DeliveryStatus { SENT, DISABLED, FAILED, INVALID }
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;

    public EmailService(JavaMailSender sender, @Value("${app.mail.enabled:false}") boolean enabled,
                        @Value("${app.mail.from:no-reply@group9.local}") String from) {
        this.sender=sender; this.enabled=enabled; this.from=from;
    }

    @Async("mailExecutor")
    public CompletableFuture<DeliveryStatus> sendNotification(String recipient, String subject, String body) {
        if (recipient == null || !recipient.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || subject == null || subject.isBlank() || subject.contains("\r") || subject.contains("\n")
                || body == null || body.isBlank()) return CompletableFuture.completedFuture(DeliveryStatus.INVALID);
        if (!enabled) {
            log.info("EMAIL disabled; notification was not sent");
            return CompletableFuture.completedFuture(DeliveryStatus.DISABLED);
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(recipient); message.setSubject(subject); message.setText(body);
        try {
            sender.send(message);
            log.info("EMAIL submitted to SMTP server");
            return CompletableFuture.completedFuture(DeliveryStatus.SENT);
        } catch (MailException error) {
            // Do not log addresses, mail content or SMTP credentials.
            log.warn("EMAIL SMTP failure: {}", error.getClass().getSimpleName());
            return CompletableFuture.completedFuture(DeliveryStatus.FAILED);
        }
    }
}
