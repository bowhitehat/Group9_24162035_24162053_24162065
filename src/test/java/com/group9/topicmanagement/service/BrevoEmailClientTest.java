package com.group9.topicmanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BrevoEmailClientTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test void sendsUtf8JsonToOneRecipientWithoutCopyingOtherStudents() throws Exception {
        AtomicReference<String> payload = new AtomicReference<>();
        AtomicReference<String> key = new AtomicReference<>();
        AtomicReference<String> method = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/smtp/email", exchange -> {
            calls.incrementAndGet();
            key.set(exchange.getRequestHeaders().getFirst("api-key"));
            method.set(exchange.getRequestMethod());
            payload.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"messageId\":\"test-message-id\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            BrevoEmailClient client = new BrevoEmailClient("test-only-key", json,
                    HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v3/smtp/email"));
            assertThat(client.send("sender@test.local", "sv@test.local", "Kết quả đề tài", "Điểm đã công bố: 8,5"))
                    .isEqualTo(EmailService.DeliveryStatus.SENT);
            JsonNode message = json.readTree(payload.get());
            assertThat(method.get()).isEqualTo("POST");
            assertThat(key.get()).isEqualTo("test-only-key");
            assertThat(message.path("sender").path("email").asText()).isEqualTo("sender@test.local");
            assertThat(message.path("to").size()).isEqualTo(1);
            assertThat(message.path("to").get(0).path("email").asText()).isEqualTo("sv@test.local");
            assertThat(message.path("subject").asText()).isEqualTo("Kết quả đề tài");
            assertThat(message.path("textContent").asText()).isEqualTo("Điểm đã công bố: 8,5");
            assertThat(message.has("cc") || message.has("bcc")).isFalse();
            assertThat(calls.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest @ValueSource(ints = {200, 302, 400, 401, 403, 429, 500, 503})
    @SuppressWarnings("unchecked")
    void rejectionDoesNotThrowOrRetry(int status) throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn("{\"messageId\":\"not-accepted\",\"message\":\"private data\"}");
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        assertThat(client(http).send("from@test.local", "sv@test.local", "Kết quả", "Đã công bố"))
                .isEqualTo(EmailService.DeliveryStatus.FAILED);
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http, times(1)).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(request.getValue().timeout()).contains(Duration.ofSeconds(15));
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"messageId\":\"\"}", "{\"messageId\":123}", "not-json", "null", ""})
    @SuppressWarnings("unchecked")
    void malformedAcknowledgementIsNotReportedAsSent(String body) throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(201);
        when(response.body()).thenReturn(body);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        assertThat(client(http).send("from@test.local", "sv@test.local", "Kết quả", "Đã công bố"))
                .isEqualTo(EmailService.DeliveryStatus.FAILED);
    }

    @Test void missingOrInjectedApiKeyNeverContactsNetwork() {
        HttpClient http = mock(HttpClient.class);
        for (String key : new String[]{"", " ", "bad\r\nkey", "x".repeat(4097)}) {
            BrevoEmailClient client = new BrevoEmailClient(key, json, http, URI.create("https://api.brevo.com/v3/smtp/email"));
            assertThat(client.send("from@test.local", "sv@test.local", "Kết quả", "Đã công bố"))
                    .isEqualTo(EmailService.DeliveryStatus.FAILED);
        }
        verifyNoInteractions(http);
    }

    @Test void timeoutDoesNotRetryBecauseProviderMayAlreadyHaveAccepted() throws Exception {
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.net.http.HttpTimeoutException("test only"));
        assertThat(client(http).send("from@test.local", "sv@test.local", "Kết quả", "Đã công bố"))
                .isEqualTo(EmailService.DeliveryStatus.FAILED);
        verify(http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test void interruptionRestoresFlagWithoutLeakingIntoBusinessRequest() throws Exception {
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("test only"));
        try {
            assertThat(client(http).send("from@test.local", "sv@test.local", "Kết quả", "Đã công bố"))
                    .isEqualTo(EmailService.DeliveryStatus.FAILED);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test void productionEndpointUsesHttpsNoRedirectAndBoundedTimeout() {
        BrevoEmailClient production = new BrevoEmailClient("test-only-key", json);
        HttpClient http = (HttpClient) org.springframework.test.util.ReflectionTestUtils.getField(production, "http");
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(production, "endpoint"))
                .isEqualTo(URI.create("https://api.brevo.com/v3/smtp/email"));
        assertThat(http.followRedirects()).isEqualTo(HttpClient.Redirect.NEVER);
        assertThat(http.connectTimeout()).contains(Duration.ofSeconds(5));
    }

    @Test @SuppressWarnings("unchecked")
    void logsNeverIncludeKeyRecipientSubjectBodyOrProviderErrorBody() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(403);
        when(response.body()).thenReturn("test-only-key private-recipient@test.local private-subject private-body");
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Logger logger = (Logger) LoggerFactory.getLogger(BrevoEmailClient.class);
        ListAppender<ILoggingEvent> captured = new ListAppender<>();
        captured.start();
        logger.addAppender(captured);
        try {
            assertThat(client(http).send("from@test.local", "private-recipient@test.local", "private-subject", "private-body"))
                    .isEqualTo(EmailService.DeliveryStatus.FAILED);
            assertThat(captured.list).isNotEmpty();
            for (ILoggingEvent event : captured.list) {
                assertThat(event.getFormattedMessage()).doesNotContain("test-only-key", "private-recipient", "private-subject", "private-body");
            }
        } finally {
            logger.detachAppender(captured);
            captured.stop();
        }
    }

    private BrevoEmailClient client(HttpClient http) {
        return new BrevoEmailClient("test-only-key", json, http, URI.create("https://api.brevo.com/v3/smtp/email"));
    }
}
