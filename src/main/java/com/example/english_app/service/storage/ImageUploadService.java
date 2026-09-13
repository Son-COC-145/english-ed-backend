package com.example.english_app.service.storage;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.service.integration.CloudinaryService;

import lombok.RequiredArgsConstructor;

/** Stores user-uploaded images and returns their public HTTPS URL. */
@Service
@RequiredArgsConstructor
public class ImageUploadService {
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    private final CloudinaryService cloudinaryService;
    private final FileContentValidator contentValidator;

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_SIZE_BYTES
                || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw ErrorCode.INVALID_IMAGE_FILE.toException();
        }

        contentValidator.validate(file);
        try {
            return cloudinaryService.uploadFile(file.getBytes(), "image",
                    "english-app/images/" + UUID.randomUUID());
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INVALID_IMAGE_FILE, "Không thể đọc file ảnh tải lên");
        }
    }
}
