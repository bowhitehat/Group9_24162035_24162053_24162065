package com.group9.topicmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:group9_brevo_health;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.demo-password=",
        "app.mail.enabled=true",
        "app.mail.provider=brevo",
        "app.mail.brevo-api-key=test-only-not-a-real-key",
        "app.mail.from=sender@test.local",
        "MAIL_SMTP_HEALTH_ENABLED=false"
})
@ActiveProfiles("test")
class BrevoMailHealthIntegrationTest {
    @LocalServerPort int port;
    @Autowired HealthContributorRegistry healthContributors;
    @MockitoBean JavaMailSender smtp;

    @Test void cloudHealthDoesNotProbeBlockedSmtpOrExposeProviderSecrets() throws Exception {
        assertThat(healthContributors.getContributor("mail")).isNull();
        assertThat(healthContributors.getContributor("db")).isNotNull();
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/actuator/health"))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
        verifyNoInteractions(smtp);
    }
}
