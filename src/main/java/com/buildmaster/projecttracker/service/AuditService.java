package com.buildmaster.projecttracker.service;

import com.buildmaster.projecttracker.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditService {

    public void logUserLogin(User user, boolean success) {
        if (success) {
            log.info("User login successful: {}", user.getEmail());
        } else {
            log.warn("User login failed: {}", user.getEmail());
        }
    }

    public void logUserRegistration(User savedUser) {
        log.info("New user registered: {}", savedUser.getEmail());
    }

}