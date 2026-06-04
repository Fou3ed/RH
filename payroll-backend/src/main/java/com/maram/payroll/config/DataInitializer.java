package com.maram.payroll.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bootstraps a default ADMIN account on first startup so the platform is usable
 * immediately after the database is provisioned. The password is hashed with the
 * application's {@link PasswordEncoder} (BCrypt) rather than baked into a migration.
 *
 * <p>Credentials are configurable via {@code maram.bootstrap.admin.*}. The default
 * password MUST be rotated before any non-local deployment.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @Value("${maram.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${maram.bootstrap.admin.email:admin@maram.local}")
    private String adminEmail;

    @Value("${maram.bootstrap.admin.password:Admin@123!}")
    private String adminPassword;

    public DataInitializer(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, adminUsername);

        if (existing != null && existing > 0) {
            log.info("Default admin user '{}' already present — skipping bootstrap.", adminUsername);
            return;
        }

        jdbc.update(
                "INSERT INTO users (username, email, password_hash, status, created_by) VALUES (?, ?, ?, 'ACTIVE', 'system')",
                adminUsername, adminEmail, passwordEncoder.encode(adminPassword));

        jdbc.update("""
                INSERT INTO user_roles (user_id, role_id, assigned_by)
                SELECT u.id, r.id, 'system'
                FROM users u, roles r
                WHERE u.username = ? AND r.code = 'ADMIN'
                """, adminUsername);

        log.warn("Bootstrapped default ADMIN account '{}'. CHANGE THE PASSWORD before any real deployment.",
                adminUsername);
    }
}
