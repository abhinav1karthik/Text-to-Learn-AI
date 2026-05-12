package com.texttolearn.audio.storage;

import com.texttolearn.audio.config.R2Properties;
import com.texttolearn.audio.error.AudioGenerationException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class R2AudioObjectStorageService implements AudioObjectStorageService {

    private static final Region R2_REGION_PLACEHOLDER = Region.of("auto");

    private final R2Properties r2Properties;
    private final S3Client s3Client;

    @Autowired
    public R2AudioObjectStorageService(R2Properties r2Properties) {
        this(r2Properties, createClient(r2Properties));
    }

    R2AudioObjectStorageService(R2Properties r2Properties, S3Client s3Client) {
        this.r2Properties = r2Properties;
        this.s3Client = s3Client;
    }

    @Override
    public boolean isConfigured() {
        return r2Properties.isConfigured();
    }

    @Override
    public byte[] get(String storageKey) {
        ensureConfigured();
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(r2Properties.bucketName())
                    .key(storageKey)
                    .build();
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            return response.asByteArray();
        } catch (S3Exception exception) {
            throw new AudioGenerationException("Failed to download saved lesson audio from R2.", exception);
        }
    }

    @Override
    public void put(String storageKey, byte[] audio, String contentType) {
        ensureConfigured();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(r2Properties.bucketName())
                    .key(storageKey)
                    .contentType(contentType)
                    .contentLength((long) audio.length)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(audio));
        } catch (S3Exception exception) {
            throw new AudioGenerationException("Failed to upload lesson audio to R2.", exception);
        }
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new AudioGenerationException("R2 storage is not configured.");
        }
    }

    private static S3Client createClient(R2Properties properties) {
        if (!properties.isConfigured()) {
            return null;
        }

        return S3Client.builder()
                .endpointOverride(URI.create(properties.effectiveEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())
                ))
                .region(R2_REGION_PLACEHOLDER)
                .forcePathStyle(true)
                .build();
    }
}
