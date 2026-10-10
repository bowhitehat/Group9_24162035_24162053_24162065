package com.group9.topicmanagement.service;

import com.group9.topicmanagement.config.UploadConfig;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class ReportFileStorageTest {
    @TempDir Path root;
    private MockMultipartFile file() {
        return new MockMultipartFile("file", "báo cáo.pdf", "application/pdf", new byte[]{1, 2, 3});
    }
    private ReportFileStorage local() {
        return new ReportFileStorage(new UploadConfig(root.toString()), "local", "", "reports", "");
    }

    @Test void localUploadAndDownloadRemainCompatible() throws Exception {
        String name = local().save("test.pdf", file());
        assertThat(name).isEqualTo("test.pdf");
        assertThat(local().exists(name)).isTrue();
        assertThat(local().read(name).getContentAsByteArray()).containsExactly(1, 2, 3);
    }

    @Test void missingFilesReturnNotFound() {
        assertThat(local().exists("missing.pdf")).isFalse();
        assertThatThrownBy(() -> local().read("missing.pdf")).isInstanceOf(NotFoundException.class);
        assertThat(local().exists(null)).isFalse();
    }

    @Test void refusesOversizedFilesAndDoesNotOverwriteExistingVersion() throws Exception {
        MockMultipartFile large = new MockMultipartFile("file", "large.pdf", "application/pdf",
                new byte[10 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> local().save("large.pdf", large)).isInstanceOf(BusinessRuleException.class);
        assertThat(Files.exists(root.resolve("large.pdf"))).isFalse();
        local().save("test.pdf", file());
        assertThatThrownBy(() -> local().save("test.pdf", file())).isInstanceOf(BusinessRuleException.class);
        assertThat(Files.readAllBytes(root.resolve("test.pdf"))).containsExactly(1, 2, 3);
    }

    @Test void pathTraversalIsRejectedWithoutReadingOtherFiles() {
        for (String name : new String[]{"../secret.pdf", "supabase:../secret.pdf", "https://other/file", "C:\\secret.pdf"}) {
            assertThat(local().exists(name)).isFalse();
            assertThatThrownBy(() -> local().read(name)).isInstanceOf(NotFoundException.class);
        }
    }

    @Test void localModeNeverTreatsCloudObjectAsLocalFile() throws Exception {
        Files.write(root.resolve("test.pdf"), new byte[]{1});
        assertThat(local().exists("supabase:test.pdf")).isFalse();
        assertThatThrownBy(() -> local().read("supabase:test.pdf")).isInstanceOf(NotFoundException.class);
    }

    @Test void configurationFailsClosed() {
        assertThatThrownBy(() -> new ReportFileStorage(new UploadConfig(root.toString()),
                "supabase", "https://project.supabase.co", "reports", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReportFileStorage(new UploadConfig(root.toString()),
                "supabase", "https://attacker.example", "reports", "test-key"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReportFileStorage(new UploadConfig(root.toString()),
                "unknown", "", "reports", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void privateHttpUploadHeadAndDownloadUseServerCredentials() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<byte[]> stored = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/storage/v1/object/", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            if (!"server-test-key".equals(exchange.getRequestHeaders().getFirst("apikey"))) {
                exchange.sendResponseHeaders(401, -1);
            } else if (exchange.getRequestMethod().equals("POST")) {
                stored.set(exchange.getRequestBody().readAllBytes());
                exchange.sendResponseHeaders(200, -1);
            } else if (exchange.getRequestMethod().equals("HEAD")) {
                exchange.sendResponseHeaders(200, -1);
            } else if (exchange.getRequestURI().getPath().equals("/storage/v1/object/authenticated/reports/test.pdf")) {
                exchange.sendResponseHeaders(200, stored.get().length);
                exchange.getResponseBody().write(stored.get());
            } else { exchange.sendResponseHeaders(404, -1); }
            exchange.close();
        });
        server.start();
        try {
            ReportFileStorage cloud = cloud(server);
            String name = cloud.save("test.pdf", file());
            assertThat(name).isEqualTo("supabase:test.pdf");
            assertThat(cloud.exists(name)).isTrue();
            assertThat(cloud.read(name).getContentAsByteArray()).containsExactly(1, 2, 3);
            assertThat(authorization.get()).isEqualTo("Bearer server-test-key");
            // Old local files are never implicitly claimed to have migrated.
            assertThat(cloud.exists("old.pdf")).isFalse();
        } finally { server.stop(0); }
    }

    @Test void remoteNotFoundAndFailuresHaveSafeMessages() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(exchange.getRequestURI().getPath().endsWith("missing.pdf") ? 404 : 401, -1);
            exchange.close();
        });
        server.start();
        try {
            ReportFileStorage cloud = cloud(server);
            assertThat(cloud.exists("supabase:missing.pdf")).isFalse();
            assertThatThrownBy(() -> cloud.read("supabase:missing.pdf")).isInstanceOf(NotFoundException.class);
            assertThatThrownBy(() -> cloud.read("supabase:bad.pdf"))
                    .isInstanceOf(BusinessRuleException.class).hasMessageNotContaining("server-test-key");
            assertThatThrownBy(() -> cloud.save("bad.pdf", file())).isInstanceOf(BusinessRuleException.class);
        } finally { server.stop(0); }
    }

    private ReportFileStorage cloud(HttpServer server) {
        return new ReportFileStorage(new UploadConfig(root.toString()), "supabase",
                "http://127.0.0.1:" + server.getAddress().getPort(), "reports", "server-test-key",
                HttpClient.newHttpClient());
    }
}
