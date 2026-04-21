package com.example.socialmedia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaFixer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaFixer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking and fixing database schema...");

        try {
            // Add allow_story_view_by_followers_only column if it doesn't exist
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS allow_story_view_by_followers_only BOOLEAN DEFAULT TRUE");
            log.info("Ensured column: allow_story_view_by_followers_only");

            // Add is_private_account column if it doesn't exist
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS is_private_account BOOLEAN DEFAULT FALSE");
            log.info("Ensured column: is_private_account");

            // Add profile_visibility column if it doesn't exist
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_visibility VARCHAR(50) DEFAULT 'PUBLIC'");
            log.info("Ensured column: profile_visibility");

            // Add password_reset_token column if it doesn't exist
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(255)");
            log.info("Ensured column: password_reset_token");

            // Add reset_token_expiry column if it doesn't exist
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token_expiry TIMESTAMP");
            log.info("Ensured column: reset_token_expiry");

        } catch (Exception e) {
            log.warn("Schema fix warning (might already exist or other issue): {}", e.getMessage());
        }
    }
}
