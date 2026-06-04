package com.maram.payroll.document.service;

import com.maram.payroll.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Thin wrapper over the MinIO client. Ensures the configured bucket exists on
 * startup and exposes put/get/remove for object keys.
 */
@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient client;
    private final String bucket;

    public MinioStorageService(MinioClient client, MinioProperties properties) {
        this.client = client;
        this.bucket = properties.bucket();
    }

    @PostConstruct
    void ensureBucket() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket '{}'", bucket);
            }
        } catch (Exception e) {
            // Don't fail startup if MinIO is unavailable — document features will error on use.
            log.warn("Could not verify/create MinIO bucket '{}': {}", bucket, e.getMessage());
        }
    }

    public void put(String objectKey, InputStream data, long size, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to upload object: " + objectKey, e);
        }
    }

    public InputStream get(String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to download object: " + objectKey, e);
        }
    }

    public void remove(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to delete object: " + objectKey, e);
        }
    }

    /** Wraps low-level storage failures. */
    public static class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
