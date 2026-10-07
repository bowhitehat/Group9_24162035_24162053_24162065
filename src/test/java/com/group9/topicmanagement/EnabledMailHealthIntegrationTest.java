package com.group9.topicmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:group9_mail_health;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.demo-password=",
        "app.mail.enabled=true",
        "spring.mail.host=127.0.0.1",
        "spring.mail.username=",
        "spring.mail.password=",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.connectiontimeout=500",
        "spring.mail.properties.mail.smtp.timeout=500"
})
@ActiveProfiles("test")
class EnabledMailHealthIntegrationTest {
    @LocalServerPort int port;
    @Autowired HealthContributorRegistry healthContributors;

    @DynamicPropertySource
    static void unavailableSmtp(DynamicPropertyRegistry registry) throws IOException {
        // Reserve a local ephemeral port, then close it: do not contact a real mail provider.
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) {
            closedPort = socket.getLocalPort();
        }
        registry.add("spring.mail.port", () -> closedPort);
    }

    @Test void enabledEmailStillReportsSmtpFailureWithoutExposingDetails() throws Exception {
        assertThat(healthContributors.getContributor("mail")).isNotNull();
        assertThat(healthContributors.getContributor("db")).isNotNull();
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/actuator/health"))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).isEqualTo("{\"status\":\"DOWN\"}");
    }
}
