package com.example.english_app.config;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import org.springframework.beans.factory.annotation.Value;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FcmConfig {
    
    // Cấu hình cho Render.com (Đọc từ Secret File bằng biến môi trường) ---
    // Trên Render, biến môi trường FIREBASE_CONFIG_PATH sẽ trỏ tới file Secret.
    @Value("${FIREBASE_CONFIG_PATH:/etc/secrets/firebase-adminsdk.json}")
    private String firebaseConfigPathEnv;

    @Value("${firebase.config.path:firebase-adminsdk.json}")
    private String firebaseConfigPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        /* 
        CẤU HÌNH ĐỂ CHẠY LOCAL (Sử dụng file trong thư mục resources) 
        Khi test dưới local, bạn hãy uncomment đoạn này và comment đoạn Render bên dưới
        log.info("Initializing Firebase Application from classpath: {}", firebaseConfigPath);
        ClassPathResource resource = new ClassPathResource(firebaseConfigPath);
        try (InputStream serviceAccount = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        }
        */

        // --- CẤU HÌNH ĐỂ DEPLOY LÊN RENDER (Sử dụng đường dẫn tuyệt đối) ---
        log.info("Initializing Firebase Application from absolute path: {}", firebaseConfigPathEnv);
        try (InputStream serviceAccount = new java.io.FileInputStream(firebaseConfigPathEnv)) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
