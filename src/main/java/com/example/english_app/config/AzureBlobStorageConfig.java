package com.example.english_app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình Azure Blob Storage cho lưu trữ âm thanh & media.
 * 
 * <pre>
 *   AZURE_STORAGE_CONNECTION_STRING → connectionString
 *   AZURE_STORAGE_CONTAINER_NAME    → containerName (mặc định: english-app-media)
 * </pre>
 */
@ConfigurationProperties(prefix = "azure.storage")
public record AzureBlobStorageConfig(
        String connectionString,
        String containerName
) {
    public String getContainerOrDefault() {
        return (containerName != null && !containerName.isBlank()) ? containerName : "english-app-media";
    }
}
