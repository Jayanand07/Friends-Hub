package com.example.socialmedia.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaFixer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Checking and fixing database schema...");

        try {
            // Add allow_story_view_by_followers_only column if it doesn't exist
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS allow_story_view_by_followers_only BOOLEAN DEFAULT TRUE");
            System.out.println("Ensured column: allow_story_view_by_followers_only");

            // Add is_private_account column if it doesn't exist
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS is_private_account BOOLEAN DEFAULT FALSE");
            System.out.println("Ensured column: is_private_account");

            // Add profile_visibility column if it doesn't exist
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_visibility VARCHAR(50) DEFAULT 'PUBLIC'");
            System.out.println("Ensured column: profile_visibility");

            // Add password_reset_token column if it doesn't exist
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(255)");
            System.out.println("Ensured column: password_reset_token");

            // Add reset_token_expiry column if it doesn't exist
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token_expiry TIMESTAMP");
            System.out.println("Ensured column: reset_token_expiry");

        } catch (Exception e) {
            System.out.println("Schema fix warning (might already exist or other issue): " + e.getMessage());
        }
    }
}
