package com.example.userservice.config;

import com.example.userservice.entity.Admin;
import com.example.userservice.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private static final String ADMIN_NAME = "GymMonitorAdmin";
    private static final String ADMIN_EMAIL = "GymMonitor@gmail.com";
    private static final String ADMIN_PASSWORD = "GymAdmin@123";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (adminRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }

        Admin admin = new Admin();
        admin.setName(ADMIN_NAME);
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        adminRepository.save(admin);
    }
}
