package com.example.english_app.service.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.PublicAccessType;
import com.example.english_app.config.AzureBlobStorageConfig;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

/**
 * Service quản lý upload và lưu trữ file tĩnh (Audio/Media) trên Azure Blob Storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AzureBlobStorageService {

    private final AzureBlobStorageConfig blobConfig;
    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;

    @PostConstruct
    void init() {
        if (blobConfig.connectionString() != null && !blobConfig.connectionString().isBlank()) {
            try {
                this.blobServiceClient = new BlobServiceClientBuilder()
                        .connectionString(blobConfig.connectionString())
                        .buildClient();

                String containerName = blobConfig.getContainerOrDefault();
                this.containerClient = blobServiceClient.getBlobContainerClient(containerName);

                if (!this.containerClient.exists()) {
                    // Tạo container và cấp quyền đọc Public cho Anonymous Blobs
                    this.containerClient.createWithResponse(null, PublicAccessType.BLOB, null, null);
                    log.info("Created Azure Blob container: {}", containerName);
                } else {
                    log.info("Connected to Azure Blob container: {}", containerName);
                }
            } catch (Exception e) {
                log.warn("Failed to initialize Azure Blob Storage Client: {}. File upload might not work until connection string is configured.", e.getMessage());
            }
        } else {
            log.info("Azure Blob Storage connection string is not set. Skipping eager initialization.");
        }
    }

    /**
     * Upload mảng byte âm thanh lên Azure Blob Storage và trả về Public URL.
     *
     * @param blobPath    Tên đường dẫn trên blob (VD: "audio/phonemes/i_male.mp3")
     * @param audioBytes  Nội dung file
     * @param contentType Content-Type (VD: "audio/mpeg")
     * @return Public URL truy cập file từ Frontend/CDN
     */
    public String uploadAudio(String blobPath, byte[] audioBytes, String contentType) {
        ensureInitialized();

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);

            BlobHttpHeaders headers = new BlobHttpHeaders();
            headers.setContentType(contentType != null ? contentType : "audio/mpeg");

            blobClient.upload(new ByteArrayInputStream(audioBytes), audioBytes.length, true);
            blobClient.setHttpHeaders(headers);

            String blobUrl = blobClient.getBlobUrl();
            log.info("Successfully uploaded audio to Azure Blob Storage: {}", blobUrl);
            return blobUrl;
        } catch (Exception e) {
            log.error("Failed to upload audio to Azure Blob: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.AUDIO_PROCESSING_FAILED);
        }
    }

    public void deleteBlob(String blobPath) {
        if (blobPath == null || blobPath.isBlank()) {
            throw new IllegalArgumentException("Blob path must not be blank");
        }
        ensureInitialized();
        containerClient.getBlobClient(blobPath).deleteIfExists();
    }

    private void ensureInitialized() {
        if (containerClient == null) {
            if (blobConfig.connectionString() == null || blobConfig.connectionString().isBlank()) {
                throw new IllegalStateException("Azure Blob Storage connection string is missing. Please set AZURE_STORAGE_CONNECTION_STRING in your environment.");
            }
            init();
            if (containerClient == null) {
                throw new IllegalStateException("Unable to connect to Azure Blob Container. Please check your connection string and network.");
            }
        }
    }
}
