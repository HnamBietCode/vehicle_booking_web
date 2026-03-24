package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.Notification;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ── Trang danh sách thông báo ──────────────────────────────────
    @GetMapping("/notifications")
    public String notificationsPage(HttpSession session, Model model) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) return "redirect:/auth/login";

        List<Notification> notifications = notificationService.getAll(loggedUser.getId());
        long unreadCount = notificationService.getUnreadCount(loggedUser.getId());
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("loggedUser", loggedUser);
        return "notifications/index";
    }

    // ── API: Đếm thông báo chưa đọc (cho navbar polling) ──────────
    @GetMapping("/api/notifications/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadCount(HttpSession session) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return ResponseEntity.status(401).build();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("count", notificationService.getUnreadCount(loggedUser.getId()));
        return ResponseEntity.ok(result);
    }

    // ── API: Lấy 20 thông báo mới nhất (cho dropdown) ──────────────
    @GetMapping("/api/notifications/latest")
    @ResponseBody
    public ResponseEntity<List<Notification>> getLatest(HttpSession session) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(notificationService.getLatest(loggedUser.getId()));
    }

    // ── API: Đánh dấu 1 thông báo đã đọc ──────────────────────────
    @PostMapping("/api/notifications/{id}/read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id, HttpSession session) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return ResponseEntity.status(401).build();
        }
        notificationService.markAsRead(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    // ── API: Đánh dấu tất cả đã đọc ───────────────────────────────
    @PostMapping("/api/notifications/read-all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAllAsRead(HttpSession session) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return ResponseEntity.status(401).build();
        }
        notificationService.markAllAsRead(loggedUser.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }
}
