package com.group9.topicmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.Locale;

/** TV3 calls this service only after the business transaction has committed. */
@Service
public class EmailService {
    // SENT means accepted by SMTP/Brevo; inbox delivery is checked in the provider's logs.
    public enum DeliveryStatus { SENT, DISABLED, FAILED, INVALID }
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;
    private final String provider;
    private final BrevoEmailClient brevo;

    @Autowired
    public EmailService(JavaMailSender sender, @Value("${app.mail.enabled:false}") boolean enabled,
                        @Value("${app.mail.from:no-reply@group9.local}") String from,
                        @Value("${app.mail.provider:smtp}") String provider, BrevoEmailClient brevo) {
        this.sender=sender; this.enabled=enabled; this.from=from;
        this.provider = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        this.brevo = brevo;
        if (!this.provider.equals("smtp") && !this.provider.equals("brevo")) {
            throw new IllegalArgumentException("MAIL_PROVIDER must be smtp or brevo");
        }
        if (enabled && this.provider.equals("brevo") && (brevo == null || !brevo.isConfigured())) {
            throw new IllegalArgumentException("Enabled Brevo delivery requires BREVO_API_KEY");
        }
    }

    public EmailService(JavaMailSender sender, boolean enabled, String from) {
        this(sender, enabled, from, "smtp", null);
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
        if (from == null || !from.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            log.warn("EMAIL sender address is invalid");
            return CompletableFuture.completedFuture(DeliveryStatus.INVALID);
        }
        if (provider.equals("brevo")) {
            return CompletableFuture.completedFuture(brevo.send(from, recipient, subject, body));
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
