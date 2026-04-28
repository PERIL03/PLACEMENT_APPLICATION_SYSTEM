package com.placement.app.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchemaMigrationRunner {

    @Bean
    CommandLineRunner migrateDriveApplicationStatusEnum(JdbcTemplate jdbcTemplate) {
        return args -> jdbcTemplate.execute(
                """
                ALTER TABLE drive_application
                MODIFY COLUMN status ENUM('APPLIED','SHORTLISTED','INTERVIEW_SCHEDULED','OFFERED','REJECTED')
                """);
    }
}
