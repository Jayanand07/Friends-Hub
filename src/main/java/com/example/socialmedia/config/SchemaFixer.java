package com.example.socialmedia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Legacy schema migration helper — ensures specific columns exist on the users table.
 * <p>
 * This is redundant when Hibernate {@code ddl-auto=update} is active (the columns are
 * defined in {@link com.example.socialmedia.entity.User} entity annotations).
 * It serves as a safety net for deployments that may have run with earlier schema versions.
 * <p>
 * Can be disabled by setting {@code app.schema-fixer.enabled=false}.
 * Once all production instances have migrated, this class can be removed entirely.
 */
@Component
@ConditionalOnProperty(name = "app.schema-fixer.enabled", havingValue = "true", matchIfMissing = true)
public class SchemaFixer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaFixer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Running legacy SchemaFixer (safe to disable via app.schema-fixer.enabled=false)...");

        try {
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS allow_story_view_by_followers_only BOOLEAN DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS is_private_account BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_visibility VARCHAR(50) DEFAULT 'PUBLIC'");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token_expiry TIMESTAMP");
            log.info("SchemaFixer completed successfully.");
        } catch (Exception e) {
            log.warn("SchemaFixer: column may already exist or other issue: {}", e.getMessage());
        }
    }
}
