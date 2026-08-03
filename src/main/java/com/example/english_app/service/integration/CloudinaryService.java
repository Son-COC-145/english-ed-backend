package com.example.english_app.service.integration;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;

    public String uploadFile(byte[] fileBytes, String resourceType, String publicId) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "public_id", publicId));
            return uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }
}
