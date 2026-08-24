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
        InputStream serviceAccount = null;
        java.io.File envFile = new java.io.File(firebaseConfigPathEnv);

        if (envFile.exists() && !envFile.isDirectory()) {
            // Chạy trên Render
            log.info("Initializing Firebase from absolute path: {}", firebaseConfigPathEnv);
            serviceAccount = new java.io.FileInputStream(envFile);
        } else {
            // Chạy dưới Local
            log.info("Initializing Firebase from classpath: {}", firebaseConfigPath);
            ClassPathResource resource = new ClassPathResource(firebaseConfigPath);
            if (resource.exists()) {
                serviceAccount = resource.getInputStream();
            } else {
                throw new java.io.FileNotFoundException("Firebase config file not found at " + firebaseConfigPathEnv + " or classpath:" + firebaseConfigPath);
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
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
