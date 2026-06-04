package com.maram.payroll.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binding for {@code maram.minio.*} (object storage connection + bucket).
 */
@ConfigurationProperties(prefix = "maram.minio")
public record MinioProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket) {
}
