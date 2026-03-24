package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Tạo thông báo mới cho user.
     */
    public Notification createNotification(Long userId, String title, String body,
                                            NotificationType type,
                                            NotificationRefType refType, Long refId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setBody(body);
        n.setType(type);
        n.setRefType(refType);
        n.setRefId(refId);
        n.setIsRead(false);
        return notificationRepository.save(n);
    }

    /**
     * Đếm số thông báo chưa đọc.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Lấy 20 thông báo mới nhất.
     */
    @Transactional(readOnly = true)
    public List<Notification> getLatest(Long userId) {
        return notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Lấy tất cả thông báo.
     */
    @Transactional(readOnly = true)
    public List<Notification> getAll(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Đánh dấu 1 thông báo đã đọc.
     */
    public void markAsRead(Long notificationId) {
        Optional<Notification> opt = notificationRepository.findById(notificationId);
        opt.ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    /**
     * Đánh dấu tất cả đã đọc.
     */
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
