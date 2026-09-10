package com.group9.topicmanagement.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class UploadConfig {
    private final Path uploadDirectory;
    public UploadConfig(@Value("${app.upload-dir}") String uploadDirectory) { this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize(); }
    @PostConstruct void createUploadDirectory() throws IOException { Files.createDirectories(uploadDirectory); }
    public Path getUploadDirectory() { return uploadDirectory; }
}
