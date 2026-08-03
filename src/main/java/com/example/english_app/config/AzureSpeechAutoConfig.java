package com.example.english_app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Kích hoạt binding {@link AzureSpeechConfig} từ application.yaml.
 *
 * <p>Tách riêng khỏi record để record giữ nguyên là POJO thuần túy,
 * không bị ràng buộc với Spring annotation.
 */
@Configuration
@EnableConfigurationProperties(AzureSpeechConfig.class)
public class AzureSpeechAutoConfig {
    // Không cần thêm bean — @EnableConfigurationProperties đã đủ.
}
