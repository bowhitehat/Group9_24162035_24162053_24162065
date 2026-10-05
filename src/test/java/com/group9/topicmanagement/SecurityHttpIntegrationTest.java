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

    @ParameterizedTest
    @CsvSource({
        "student1,/topics?page=-1,400", "student1,/topics?size=0,400",
        "student1,/topics?size=101,400", "student1,/topics?status=BAD,400",
        "student1,/topics/detail/999999,404", "student1,/registrations?page=-1,400",
        "student1,/evaluations/my-result,400", "student1,/evaluations/my-result?topicId=no-number,400",
        "lecturer1,/evaluations/topic/1?type=BAD,400", "admin,/unknown-path,404"
    })
    void invalidRequestsReturnClientErrorInsteadOf500(String username, String path, int expected) throws Exception {
        var response = get(login(username).client(), path);
        assertThat(response.statusCode()).isEqualTo(expected);
        assertThat(response.body()).doesNotContain("500 ·");
    }

    @Test void missingRegistrationIdIs400() throws Exception {
        var session = login("student1");
        var response = post(session.client(), "/reports/submit", "_csrf=" + encode(session.csrf()));
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("chọn đăng ký đề tài");
    }

    @Test void shortPasswordIs400AndOriginalPasswordStillWorks() throws Exception {
        var session = login("student1");
        var response = post(session.client(), "/profile/password", "currentPassword=" + encode("SecurityTest@123")
                + "&newPassword=x&_csrf=" + encode(session.csrf()));
        assertThat(response.statusCode()).isEqualTo(400);
        login("student1");
    }

    @Test void blankAdminFormsShowValidationMessages() throws Exception {
        var session = login("admin");
        var users = post(session.client(), "/admin/users", "_csrf=" + encode(session.csrf()));
        assertThat(users.statusCode()).isEqualTo(200);
        assertThat(users.body()).contains("Tên đăng nhập là bắt buộc", "Họ tên là bắt buộc",
                "Email là bắt buộc", "Chọn ít nhất một vai trò", "Mật khẩu là bắt buộc", "alert-danger");
        var departments = post(session.client(), "/admin/departments", "_csrf=" + encode(session.csrf()));
        assertThat(departments.statusCode()).isEqualTo(200);
        assertThat(departments.body()).contains("Mã bộ môn là bắt buộc", "Tên bộ môn là bắt buộc", "alert-danger");
    }

    @Test void lecturerNavbarUsesAssignedTasksAndManagerKeepsManagementLink() throws Exception {
        var session = login("lecturer1");
        var html = get(session.client(), "/dashboard").body();
        assertThat(html).containsPattern("href=\"/reviewer-assignments/my-assignments\">Phản biện</a>");
        assertThat(html).doesNotContain("href=\"/reviewer-assignments\">Phản biện</a>");
        assertThat(get(session.client(), "/reviewer-assignments/my-assignments").statusCode()).isEqualTo(200);
        assertThat(get(login("faculty").client(), "/dashboard").body())
                .containsPattern("href=\"/reviewer-assignments\">Phản biện</a>");
    }

    @Test void successfulDepartmentCreationShowsFlashMessageOnce() throws Exception {
        var session = login("admin");
        var response = post(session.client(), "/admin/departments", "code=QAFLASH&name=QA+Flash&_csrf=" + encode(session.csrf()));
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(get(session.client(), "/admin/departments").body()).contains("Đã tạo bộ môn", "alert-success");
        assertThat(get(session.client(), "/admin/departments").body()).doesNotContain("Đã tạo bộ môn");
    }

    @Test void oversizedMultipartIs413OnRealTomcat() throws Exception {
        var session = login("student1");
        String boundary = "Group9UploadTest";
        byte[] head = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"large.pdf\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        var payload = new java.io.ByteArrayOutputStream();
        payload.write(head); payload.write(new byte[11 * 1024 * 1024]); payload.write(tail);
        var response = session.client().send(HttpRequest.newBuilder(uri("/reports/submit"))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-CSRF-TOKEN", session.csrf()).POST(HttpRequest.BodyPublishers.ofByteArray(payload.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("10 MB", "Quay lại nộp báo cáo");
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
