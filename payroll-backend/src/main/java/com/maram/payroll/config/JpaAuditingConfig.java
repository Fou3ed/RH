package com.maram.payroll.config;

import com.maram.payroll.auth.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Enables Spring Data JPA auditing and resolves the "current auditor" (the
 * authenticated username) for {@code @CreatedBy}/{@code @LastModifiedBy} fields.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(SecurityUtils.getCurrentUsername().orElse("system"));
    }
}
