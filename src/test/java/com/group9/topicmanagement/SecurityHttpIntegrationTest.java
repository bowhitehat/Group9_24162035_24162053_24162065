package com.group9.topicmanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// A real servlet server is necessary: MockMvc does not execute Security's forwards.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:group9_security_http;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.demo-password=SecurityTest@123"
})
@ActiveProfiles("test")
class SecurityHttpIntegrationTest {
    @LocalServerPort int port;

    @ParameterizedTest
    @CsvSource({
            "student1,/topics/approve/1",
            "student1,/topics/reject/1",
            "student1,/topics/publish/1",
            "student1,/registrations/approve/1",
            "student1,/admin/users",
            "student1,/faculty/periods",
            "lecturer1,/topics/approve/1",
            "lecturer1,/registrations/approve/1",
            "lecturer1,/admin/users",
            "faculty,/admin/users",
            "admin,/groups/create"
    })
    void forbiddenPostWithValidCsrfReturns403Not500(String username, String path) throws Exception {
        Session session = login(username);
        HttpResponse<String> response = post(session.client(), path, "_csrf=" + encode(session.csrf()));
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("403 · Không có quyền").doesNotContain("500 ·");
        assertThat(response.headers().firstValue("Location")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"student1,/admin/users", "lecturer1,/faculty/periods", "faculty,/admin/departments", "admin,/groups/my-group"})
    void forbiddenGetStillReturns403(String username, String path) throws Exception {
        HttpResponse<String> response = get(login(username).client(), path);
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("403 · Không có quyền");
    }

    @ParameterizedTest
    @CsvSource({"admin,/admin/users", "faculty,/faculty/periods", "lecturer1,/topics/create", "student1,/groups/my-group"})
    void eachRoleCanStillUseItsAllowedPage(String username, String path) throws Exception {
        assertThat(get(login(username).client(), path).statusCode()).isEqualTo(200);
    }

    @Test void missingCsrfOnAnOtherwiseAllowedPostReturns403() throws Exception {
        HttpResponse<String> response = post(login("admin").client(), "/admin/users", "username=not-created");
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("403 · Không có quyền").doesNotContain("500 ·");
    }

    @Test void anonymousGetRedirectsToLogin() throws Exception {
        HttpResponse<String> response = get(client(), "/admin/users");
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location")).hasValue("http://localhost:" + port + "/login");
    }

    @Test void studentHasRegistrationLinksOnDesktopAndMobile() throws Exception {
        String html = get(login("student1").client(), "/dashboard").body();
        assertThat(html).containsPattern("href=\"/registrations\"[^>]*>Đăng ký đề tài</a>");
        assertThat(Pattern.compile("href=\"/registrations\"").matcher(html).results().count()).isEqualTo(2);
        assertThat(html).doesNotContain("Quản lý đăng ký", "Duyệt đăng ký", "CORE &amp; ADMIN");
    }

    @Test void managerKeepsItsRegistrationManagementLink() throws Exception {
        String html = get(login("faculty").client(), "/dashboard").body();
        assertThat(html).contains("Quản lý đăng ký", "Duyệt đăng ký").doesNotContain(">Đăng ký đề tài</a>");
    }

    private Session login(String username) throws Exception {
        HttpClient client = client();
        String token = csrf(get(client, "/login").body());
        HttpResponse<String> response = post(client, "/login", "username=" + encode(username)
                + "&password=" + encode("SecurityTest@123") + "&_csrf=" + encode(token));
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location").orElseThrow()).endsWith("/dashboard");
        return new Session(client, csrf(get(client, "/dashboard").body()));
    }

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(HttpClient client, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private String csrf(String html) {
        var input = Pattern.compile("<input\\b[^>]*name=\"_csrf\"[^>]*>").matcher(html);
        assertThat(input.find()).as("CSRF field in rendered form").isTrue();
        var value = Pattern.compile("value=\"([^\"]+)\"").matcher(input.group());
        assertThat(value.find()).isTrue();
        return value.group(1);
    }

    private record Session(HttpClient client, String csrf) {}
}
