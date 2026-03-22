package com.bookvehicle.example.sr.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;

@Configuration
public class FirebaseConfig {

    public FirebaseConfig(@Value("${app.fcm.service-account:}") String serviceAccountPath) {
        try {
            if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
                return;
            }
            File file = new File(serviceAccountPath);
            if (!file.exists()) {
                return;
            }
            FileInputStream serviceAccount = new FileInputStream(file);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        } catch (Exception ignored) {
        }
    }
}
