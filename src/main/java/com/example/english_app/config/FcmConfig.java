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
    
    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        log.info("Initializing Firebase Application from path: {}", firebaseConfigPath);
        ClassPathResource resource = new ClassPathResource(firebaseConfigPath);

        try (InputStream serviceAccount = resource.getInputStream()) {
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
