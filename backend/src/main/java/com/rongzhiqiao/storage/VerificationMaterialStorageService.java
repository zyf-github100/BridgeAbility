package com.rongzhiqiao.storage;

import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.config.StorageProperties;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
public class VerificationMaterialStorageService implements DisposableBean {

    private static final String STORAGE_NAMESPACE = "enterprise-verification";
    private static final String R2_LOCATION_PREFIX = "r2://";

    private final StorageProperties storageProperties;

    private volatile S3Client s3Client;

    public String store(Long userId, String originalFileName, MultipartFile file) {
        String provider = resolveWriteProvider();
        if ("r2".equals(provider)) {
            return storeToR2(userId, originalFileName, file);
        }
        return storeLocally(userId, originalFileName, file);
    }

    public Resource load(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new BusinessException(4004, "verification material file not found");
        }
        if (isR2Location(storagePath)) {
            return loadFromR2(storagePath);
        }
        return loadFromLocal(storagePath);
    }

    public void deleteIfExists(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        if (isR2Location(storagePath)) {
            deleteFromR2(storagePath);
            return;
        }
        deleteFromLocal(storagePath);
    }

    @Override
    public void destroy() {
        S3Client client = s3Client;
        if (client != null) {
            client.close();
        }
    }

    private String storeLocally(Long userId, String originalFileName, MultipartFile file) {
        Path userDir = Path.of(storageProperties.getLocalRoot(), STORAGE_NAMESPACE, String.valueOf(userId));
        try {
            Files.createDirectories(userDir);
            Path target = userDir.resolve(UUID.randomUUID() + "-" + originalFileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException exception) {
            throw new BusinessException(5000, "material upload failed");
        }
    }

    private String storeToR2(Long userId, String originalFileName, MultipartFile file) {
        StorageProperties.R2 r2 = requireR2Configuration();
        String objectKey = STORAGE_NAMESPACE + "/" + userId + "/" + UUID.randomUUID() + "-" + originalFileName;
        try {
            s3Client().putObject(
                    PutObjectRequest.builder()
                            .bucket(r2.getBucket())
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            return R2_LOCATION_PREFIX + r2.getBucket() + "/" + objectKey;
        } catch (IOException | S3Exception exception) {
            throw new BusinessException(5000, "material upload failed");
        }
    }

    private Resource loadFromLocal(String storagePath) {
        Path path = Path.of(storagePath);
        if (!Files.exists(path)) {
            throw new BusinessException(4004, "verification material file not found");
        }
        return new FileSystemResource(path);
    }

    private Resource loadFromR2(String storagePath) {
        R2Location location = parseR2Location(storagePath);
        try {
            ResponseInputStream<GetObjectResponse> inputStream = s3Client().getObject(
                    GetObjectRequest.builder()
                            .bucket(location.bucket())
                            .key(location.key())
                            .build()
            );
            return new InputStreamResource(inputStream);
        } catch (NoSuchKeyException exception) {
            throw new BusinessException(4004, "verification material file not found");
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new BusinessException(4004, "verification material file not found");
            }
            throw new BusinessException(5000, "verification material file load failed");
        }
    }

    private void deleteFromLocal(String storagePath) {
        try {
            Files.deleteIfExists(Path.of(storagePath));
        } catch (IOException ignored) {
            // Ignore storage cleanup failure to preserve the current API behavior.
        }
    }

    private void deleteFromR2(String storagePath) {
        try {
            R2Location location = parseR2Location(storagePath);
            s3Client().deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(location.bucket())
                            .key(location.key())
                            .build()
            );
        } catch (BusinessException | S3Exception ignored) {
            // Ignore storage cleanup failure to preserve the current API behavior.
        }
    }

    private String resolveWriteProvider() {
        String configured = storageProperties.getProvider() == null
                ? "auto"
                : storageProperties.getProvider().trim().toLowerCase();
        if ("local".equals(configured)) {
            return "local";
        }
        if ("r2".equals(configured)) {
            requireR2Configuration();
            return "r2";
        }
        if ("auto".equals(configured) || configured.isBlank()) {
            return storageProperties.getR2().isConfigured() ? "r2" : "local";
        }
        throw new BusinessException(5000, "unsupported storage provider");
    }

    private StorageProperties.R2 requireR2Configuration() {
        StorageProperties.R2 r2 = storageProperties.getR2();
        if (!r2.isConfigured()) {
            throw new BusinessException(5000, "R2 storage is not configured");
        }
        return r2;
    }

    private S3Client s3Client() {
        S3Client client = s3Client;
        if (client != null) {
            return client;
        }
        synchronized (this) {
            if (s3Client == null) {
                StorageProperties.R2 r2 = requireR2Configuration();
                s3Client = S3Client.builder()
                        .endpointOverride(URI.create(r2.getEndpoint()))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(r2.getAccessKeyId(), r2.getSecretAccessKey())
                        ))
                        .region(Region.of(r2.getRegion()))
                        .serviceConfiguration(S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .chunkedEncodingEnabled(false)
                                .build())
                        .build();
            }
            return s3Client;
        }
    }

    private boolean isR2Location(String storagePath) {
        return storagePath.startsWith(R2_LOCATION_PREFIX);
    }

    private R2Location parseR2Location(String storagePath) {
        String value = storagePath.substring(R2_LOCATION_PREFIX.length());
        int separatorIndex = value.indexOf('/');
        if (separatorIndex <= 0 || separatorIndex == value.length() - 1) {
            throw new BusinessException(4004, "verification material file not found");
        }
        return new R2Location(
                value.substring(0, separatorIndex),
                value.substring(separatorIndex + 1)
        );
    }

    private record R2Location(String bucket, String key) {
    }
}
