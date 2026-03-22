package com.bookvehicle.example.sr.controller.mobile;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.DeviceTokenRequest;
import com.bookvehicle.example.sr.model.DeviceToken;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.repository.DeviceTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile/devices")
public class DeviceTokenController {

    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceTokenController(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody DeviceTokenRequest req,
                                      HttpSession session,
                                      HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null) {
            return ResponseEntity.status(401).build();
        }
        if (req.getToken() == null || req.getToken().isBlank()) {
            return ResponseEntity.badRequest().body("Token khong hop le.");
        }

        DeviceToken token = deviceTokenRepository.findByUserIdAndToken(loggedUser.getId(), req.getToken())
                .orElseGet(DeviceToken::new);
        token.setUserId(loggedUser.getId());
        token.setToken(req.getToken().trim());
        token.setPlatform(req.getPlatform());
        deviceTokenRepository.save(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unregister")
    public ResponseEntity<?> unregister(@RequestBody DeviceTokenRequest req,
                                        HttpSession session,
                                        HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null) {
            return ResponseEntity.status(401).build();
        }
        if (req.getToken() == null || req.getToken().isBlank()) {
            return ResponseEntity.badRequest().body("Token khong hop le.");
        }
        deviceTokenRepository.deleteByUserIdAndToken(loggedUser.getId(), req.getToken().trim());
        return ResponseEntity.ok().build();
    }
}
