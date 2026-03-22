package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.model.DeviceToken;
import com.bookvehicle.example.sr.repository.DeviceTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    public PushNotificationService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    public void notifyUsers(List<Long> userIds, String title, String body, Map<String, String> data) {
        if (userIds == null || userIds.isEmpty()) return;
        List<DeviceToken> tokens = userIds.stream()
                .flatMap(id -> deviceTokenRepository.findByUserId(id).stream())
                .collect(Collectors.toList());
        if (tokens.isEmpty()) return;

        for (DeviceToken t : tokens) {
            try {
                Message.Builder builder = Message.builder()
                        .setToken(t.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build());
                if (data != null && !data.isEmpty()) {
                    builder.putAllData(data);
                }
                FirebaseMessaging.getInstance().send(builder.build());
            } catch (Exception ignored) {
            }
        }
    }
}
