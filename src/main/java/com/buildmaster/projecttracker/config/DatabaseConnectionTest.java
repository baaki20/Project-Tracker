package com.buildmaster.projecttracker.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseConnectionTest implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            log.info("=== DATABASE CONNECTION TEST ===");
            log.info("Database URL: {}", metaData.getURL());
            log.info("Database Name: {}", metaData.getDatabaseProductName());
            log.info("Database Version: {}", metaData.getDatabaseProductVersion());
            log.info("Driver Name: {}", metaData.getDriverName());
            log.info("Driver Version: {}", metaData.getDriverVersion());
            log.info("Connection successful!");
            log.info("================================");
        } catch (Exception e) {
            log.error("=== DATABASE CONNECTION FAILED ===");
            log.error("Error: {}", e.getMessage());
            log.error("===================================");
        }
    }
}