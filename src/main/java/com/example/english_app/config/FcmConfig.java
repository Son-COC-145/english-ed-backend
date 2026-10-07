package com.example.english_app.config;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;

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

    @Value("${FIREBASE_CREDENTIALS:#{null}}")
    private String firebaseCredentialsJson;

    @Bean
    @Nullable
    public FirebaseApp firebaseApp() {
        InputStream serviceAccount = null;
        java.io.File envFile = new java.io.File(firebaseConfigPathEnv);

        try {
            if (firebaseCredentialsJson != null && !firebaseCredentialsJson.trim().isEmpty()) {
                log.info("Initializing Firebase from FIREBASE_CREDENTIALS environment variable.");
                serviceAccount = new java.io.ByteArrayInputStream(firebaseCredentialsJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } else if (envFile.exists() && !envFile.isDirectory()) {
                // Chạy trên Render hoặc môi trường cloud mount file
                log.info("Initializing Firebase from absolute path: {}", firebaseConfigPathEnv);
                serviceAccount = new java.io.FileInputStream(envFile);
            } else {
                // Chạy dưới Local
                ClassPathResource resource = new ClassPathResource(firebaseConfigPath);
                if (resource.exists()) {
                    log.info("Initializing Firebase from classpath: {}", firebaseConfigPath);
                    serviceAccount = resource.getInputStream();
                } else {
                    log.warn("Firebase config not found! FIREBASE_CREDENTIALS is empty, {} does not exist, and classpath:{} is missing. FCM disabled.",
                            firebaseConfigPathEnv, firebaseConfigPath);
                    return null;
                }
            }

            try (InputStream is = serviceAccount) {
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(is)).build();

                if (FirebaseApp.getApps().isEmpty()) {
                    return FirebaseApp.initializeApp(options);
                }
                return FirebaseApp.getInstance();
            }
        } catch (Exception e) {
            log.error("Failed to initialize FirebaseApp: {}. Push notifications will be disabled.", e.getMessage());
            return null;
        }
    }

    @Bean
    @Nullable
    public FirebaseMessaging firebaseMessaging(@Nullable FirebaseApp firebaseApp) {
        if (firebaseApp == null) {
            log.warn("FirebaseApp is not available. FirebaseMessaging bean is disabled.");
            return null;
        }
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
