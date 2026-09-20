package com.group9.topicmanagement.config;

import com.group9.topicmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataInitializer implements CommandLineRunner {
    private final UserService users;
    private final String demoPassword;

    public DemoDataInitializer(UserService users, @Value("${app.demo-password:}") String demoPassword) {
        this.users = users;
        this.demoPassword = demoPassword;
    }

    @Override public void run(String... args) { users.ensureDemoUsers(demoPassword); }
}
