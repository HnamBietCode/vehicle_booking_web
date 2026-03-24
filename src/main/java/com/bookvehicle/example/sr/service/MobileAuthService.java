package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.model.UserSession;
import com.bookvehicle.example.sr.repository.UserRepository;
import com.bookvehicle.example.sr.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MobileAuthService {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;

    public MobileAuthService(AuthService authService,
                             UserRepository userRepository,
                             UserSessionRepository userSessionRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
    }

    public TokenResult login(String email, String password, HttpServletRequest request) {
        AuthService.LoginResult loginResult = authService.attemptLogin(email, password);
        if (!loginResult.ok()) {
            return TokenResult.error(loginResult.message());
        }
        User user = loginResult.user();
        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();
        String tokenHash = DigestUtils.sha256Hex(rawToken);

        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setTokenHash(tokenHash);
        session.setIpAddress(request.getRemoteAddr());
        session.setUserAgent(request.getHeader("User-Agent"));
        session.setExpiresAt(LocalDateTime.now().plusDays(7));
        userSessionRepository.save(session);

        return TokenResult.success(rawToken, user);
    }

    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        String tokenHash = DigestUtils.sha256Hex(rawToken);
        userSessionRepository.deleteByTokenHash(tokenHash);
    }

    public void logoutAll(Long userId) {
        if (userId == null) return;
        userSessionRepository.deleteByUserId(userId);
    }

    public TokenResult refresh(String rawToken, HttpServletRequest request) {
        if (rawToken == null || rawToken.isBlank()) {
            return TokenResult.error("Token khong hop le.");
        }
        String tokenHash = DigestUtils.sha256Hex(rawToken);
        var sessionOpt = userSessionRepository.findValidByTokenHash(tokenHash, LocalDateTime.now());
        if (sessionOpt.isEmpty()) {
            return TokenResult.error("Token het han hoac khong ton tai.");
        }
        UserSession session = sessionOpt.get();
        userSessionRepository.deleteByTokenHash(tokenHash);

        String newRaw = UUID.randomUUID() + "-" + UUID.randomUUID();
        String newHash = DigestUtils.sha256Hex(newRaw);
        UserSession newSession = new UserSession();
        newSession.setUserId(session.getUserId());
        newSession.setTokenHash(newHash);
        newSession.setIpAddress(request.getRemoteAddr());
        newSession.setUserAgent(request.getHeader("User-Agent"));
        newSession.setExpiresAt(LocalDateTime.now().plusDays(7));
        userSessionRepository.save(newSession);

        User user = userRepository.findById(session.getUserId()).orElse(null);
        if (user == null) {
            return TokenResult.error("Nguoi dung khong ton tai.");
        }
        return TokenResult.success(newRaw, user);
    }

    public User resolveUser(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        String tokenHash = DigestUtils.sha256Hex(rawToken);
        return userSessionRepository.findValidByTokenHash(tokenHash, LocalDateTime.now())
                .flatMap(s -> userRepository.findById(s.getUserId()))
                .orElse(null);
    }

    public record TokenResult(boolean ok, String message, String token, User user) {
        public static TokenResult success(String token, User user) {
            return new TokenResult(true, "OK", token, user);
        }
        public static TokenResult error(String msg) {
            return new TokenResult(false, msg, null, null);
        }
    }
}
