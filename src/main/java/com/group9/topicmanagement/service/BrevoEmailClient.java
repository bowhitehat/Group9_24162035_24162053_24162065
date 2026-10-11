package com.group9.topicmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** HTTPS delivery for hosting plans that do not permit outbound SMTP. */
@Component
public class BrevoEmailClient {
    private static final Logger log = LoggerFactory.getLogger(BrevoEmailClient.class);
    private static final URI ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");
    private final String apiKey;
    private final ObjectMapper json;
    private final HttpClient http;
    private final URI endpoint;

    @Autowired
    public BrevoEmailClient(@Value("${app.mail.brevo-api-key:}") String apiKey, ObjectMapper json) {
        this(apiKey, json, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build(), ENDPOINT);
    }

    // Only tests can select a loopback server. The production URL is never environment-controlled.
    BrevoEmailClient(String apiKey, ObjectMapper json, HttpClient http, URI endpoint) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.json = json;
        this.http = http;
        this.endpoint = endpoint;
    }

    boolean isConfigured() {
        return !apiKey.isBlank() && apiKey.length() <= 4096
                && !apiKey.contains("\r") && !apiKey.contains("\n");
    }

    public EmailService.DeliveryStatus send(String from, String recipient, String subject, String body) {
        if (!isConfigured()) {
            log.warn("EMAIL Brevo API key is not configured");
            return EmailService.DeliveryStatus.FAILED;
        }
        try {
            String payload = json.writeValueAsString(Map.of(
                    "sender", Map.of("name", "Group 9", "email", from),
                    "to", List.of(Map.of("email", recipient)),
                    "subject", subject, "textContent", body));
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(15))
                    .header("api-key", apiKey).header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 201 && response.body().length() <= 8192) {
                JsonNode result = json.readTree(response.body());
                JsonNode messageId = result == null ? null : result.get("messageId");
                if (messageId != null && messageId.isTextual() && !messageId.asText().isBlank()) {
                    // Accepted by the provider, not proof that the recipient's inbox received it.
                    log.info("EMAIL submitted to Brevo API");
                    return EmailService.DeliveryStatus.SENT;
                }
            }
            // Never log the response body: errors can contain recipients, content or credentials.
            log.warn("EMAIL Brevo API did not accept message; HTTP {}", response.statusCode());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            log.warn("EMAIL Brevo delivery interrupted");
        } catch (IOException | RuntimeException failure) {
            log.warn("EMAIL Brevo delivery failure: {}", failure.getClass().getSimpleName());
        }
        // No automatic retry: a timeout may occur after the provider already accepted the email.
        return EmailService.DeliveryStatus.FAILED;
    }
}
