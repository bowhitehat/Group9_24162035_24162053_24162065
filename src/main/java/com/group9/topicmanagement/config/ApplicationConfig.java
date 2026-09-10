package com.group9.topicmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class ApplicationConfig {
    @Bean Clock clock() { return Clock.systemDefaultZone(); }
}
