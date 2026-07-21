package com.example.english_app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationService {
    private final CloudinaryService cloudinaryService;
    private final RestClient restClient = RestClient.create();

    public String generateImage(String word, String topic) {
        String prompt = "flat vector icon of " + word + " in the context of " + topic + ", white background";
        String url = "https://image.pollinations.ai/prompt/" + prompt.replace(" ", "%20");
        try {
            byte[] imageBytes = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(byte[].class);
            String publicId = "images/" + word.replaceAll("\\s+", "_") + "_"
                    + UUID.randomUUID().toString().substring(0, 5);
            return cloudinaryService.uploadFile(imageBytes, "image", publicId);
        } catch (Exception e) {
            log.error("Lỗi khi gọi Pollinations API", e);
            throw new RuntimeException("Image Generation Error");
        }
    }
}
