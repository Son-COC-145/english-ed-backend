package com.example.english_app.service.integration;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.english_app.service.storage.StorageLifecycleService;
import com.example.english_app.repository.storage.StoredFileRepository.StoredFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;
    private final StorageLifecycleService lifecycle;

    public record UploadedFile(String url, String publicId, String resourceType) {}

    public String uploadFile(byte[] fileBytes, String resourceType, String publicId) {
        return uploadWithMetadata(fileBytes, resourceType, publicId).url();
    }

    public UploadedFile uploadWithMetadata(byte[] fileBytes, String resourceType, String keyPrefix) {
        if (keyPrefix == null || keyPrefix.isBlank()) throw new IllegalArgumentException("Public id is required");
        String publicId = keyPrefix + "_" + UUID.randomUUID();
        StoredFile file = lifecycle.begin("CLOUDINARY", publicId, resourceType, fileBytes);
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "public_id", publicId,
                    "overwrite", false));
            if (!publicId.equals(uploadResult.get("public_id"))) {
                throw new IllegalStateException("Cloudinary returned an unexpected public id");
            }
            String url = uploadResult.get("secure_url").toString();
            lifecycle.uploaded(file, url);
            return new UploadedFile(url, publicId, resourceType);
        } catch (Exception e) {
            lifecycle.failed(file);
            log.error("Failed to upload file to Cloudinary", e);
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    public void deleteFile(String publicId, String resourceType) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "invalidate", true));
            if (!"ok".equals(result.get("result")) && !"not found".equals(result.get("result"))) {
                throw new IllegalStateException("Cloudinary did not confirm deletion");
            }
        } catch (Exception e) {
            log.error("Failed to delete Cloudinary file: publicId={}", publicId, e);
            throw new RuntimeException("Failed to delete file from Cloudinary", e);
        }
    }
}

