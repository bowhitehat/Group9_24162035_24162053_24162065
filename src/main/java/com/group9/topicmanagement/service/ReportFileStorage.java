package com.group9.topicmanagement.service;

import com.group9.topicmanagement.config.UploadConfig;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/** Private server-side storage; callers must authorize the report before reading it. */
@Service
public class ReportFileStorage {
    private static final String CLOUD_PREFIX = "supabase:";
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private final UploadConfig uploads;
    private final boolean cloud;
    private final URI endpoint;
    private final String bucket;
    private final String key;
    private final HttpClient client;

    @Autowired
    public ReportFileStorage(UploadConfig uploads,
            @Value("${app.storage.provider:local}") String provider,
            @Value("${app.storage.supabase-url:}") String url,
            @Value("${app.storage.supabase-bucket:reports}") String bucket,
            @Value("${app.storage.supabase-key:}") String key) {
        this(uploads, provider, url, bucket, key,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.NEVER).build());
        if (cloud && (!"https".equals(endpoint.getScheme()) || endpoint.getHost() == null
                || !endpoint.getHost().endsWith(".supabase.co")
                || endpoint.getRawQuery() != null || endpoint.getUserInfo() != null
                || endpoint.getFragment() != null || endpoint.getPort() != -1
                || !endpoint.getPath().isEmpty())) {
            throw new IllegalArgumentException("SUPABASE_URL phải là URL HTTPS của project Supabase");
        }
    }

    // Explicit client injection permits isolated HTTP tests without real cloud credentials.
    ReportFileStorage(UploadConfig uploads, String provider, String url, String bucket,
                      String key, HttpClient client) {
        if (!"local".equals(provider) && !"supabase".equals(provider)) {
            throw new IllegalArgumentException("STORAGE_PROVIDER chỉ nhận local hoặc supabase");
        }
        this.uploads = uploads;
        this.cloud = "supabase".equals(provider);
        this.endpoint = URI.create(url.replaceAll("/+$", ""));
        this.bucket = bucket;
        this.key = key;
        this.client = client;
        if (cloud && (url.isBlank() || key.isBlank() || !bucket.matches("[a-zA-Z0-9_-]+"))) {
            throw new IllegalArgumentException("Cần cấu hình SUPABASE_URL, SUPABASE_STORAGE_KEY và bucket riêng tư");
        }
    }

    public String save(String name, MultipartFile file) {
        validateName(name);
        try {
            if (file.getSize() > MAX_BYTES) throw new BusinessRuleException("Tập tin vượt quá 10 MB");
            if (cloud) {
                byte[] bytes;
                try (var stream = file.getInputStream()) { bytes = stream.readNBytes(MAX_BYTES + 1); }
                if (bytes.length > MAX_BYTES) throw new BusinessRuleException("Tập tin vượt quá 10 MB");
                HttpRequest request = request("/object/", name)
                        .header("Content-Type", "application/octet-stream")
                        .header("x-upsert", "false")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
                HttpResponse<Void> response = send(request, HttpResponse.BodyHandlers.discarding());
                requireSuccess(response.statusCode());
                return CLOUD_PREFIX + name;
            }
            try (var stream = file.getInputStream()) {
                Files.copy(stream, localPath(name));
            }
            return name;
        } catch (IOException e) {
            throw unavailable();
        }
    }

    public boolean exists(String storedName) {
        if (storedName == null) return false;
        String name = stripPrefix(storedName);
        if (!validName(name)) return false;
        if (!storedName.startsWith(CLOUD_PREFIX)) {
            Path path = localPath(name);
            return Files.isRegularFile(path) && Files.isReadable(path);
        }
        if (!cloud) return false;
        HttpResponse<Void> response = send(request("/object/authenticated/", name)
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 404) return false;
        requireSuccess(response.statusCode());
        return true;
    }

    public Resource read(String storedName) {
        if (storedName == null) throw missing();
        String name = stripPrefix(storedName);
        validateName(name);
        if (storedName.startsWith(CLOUD_PREFIX)) {
            if (!cloud) throw missing();
            HttpResponse<java.io.InputStream> response = send(request("/object/authenticated/", name)
                    .GET().build(), HttpResponse.BodyHandlers.ofInputStream());
            try (var stream = response.body()) {
                if (response.statusCode() == 404) throw missing();
                requireSuccess(response.statusCode());
                byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) throw new BusinessRuleException("Tập tin vượt quá 10 MB");
                return new ByteArrayResource(bytes);
            } catch (IOException e) { throw unavailable(); }
        }
        try {
            Resource resource = new UrlResource(localPath(name).toUri());
            if (!resource.exists() || !resource.isReadable()) throw missing();
            return resource;
        } catch (IOException e) { throw missing(); }
    }

    private HttpRequest.Builder request(String route, String name) {
        // Only validated bucket and generated filenames are placed into the URL.
        return HttpRequest.newBuilder(URI.create(endpoint + "/storage/v1" + route + bucket + "/" + name))
                .timeout(Duration.ofSeconds(30)).header("apikey", key)
                .header("Authorization", "Bearer " + key);
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        try { return client.send(request, handler); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException e) { throw unavailable(); }
    }

    private Path localPath(String name) {
        validateName(name);
        Path root = uploads.getUploadDirectory().toAbsolutePath().normalize();
        Path path = root.resolve(name).normalize();
        if (!path.startsWith(root)) throw missing();
        return path;
    }

    private static String stripPrefix(String name) {
        return name.startsWith(CLOUD_PREFIX) ? name.substring(CLOUD_PREFIX.length()) : name;
    }
    private static boolean validName(String name) {
        return name != null && name.length() <= 240 && !name.contains("..")
                && name.matches("[a-zA-Z0-9][a-zA-Z0-9._-]*");
    }
    private static void validateName(String name) { if (!validName(name)) throw missing(); }
    private static void requireSuccess(int status) { if (status < 200 || status >= 300) throw unavailable(); }
    private static BusinessRuleException unavailable() {
        return new BusinessRuleException("Không thể kết nối kho báo cáo. Vui lòng thử lại sau hoặc liên hệ quản trị viên.");
    }
    private static NotFoundException missing() {
        return new NotFoundException("Tệp báo cáo không còn trên máy chủ. Vui lòng liên hệ nhóm trưởng nộp lại file gốc.");
    }
}
