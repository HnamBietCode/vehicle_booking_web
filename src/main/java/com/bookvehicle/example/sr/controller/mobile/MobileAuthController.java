package com.bookvehicle.example.sr.controller.mobile;

import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.service.MobileAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile/auth")
public class MobileAuthController {

    private final MobileAuthService mobileAuthService;

    public MobileAuthController(MobileAuthService mobileAuthService) {
        this.mobileAuthService = mobileAuthService;
    }

    public record LoginRequest(String email, String password) {}
    public record LoginResponse(boolean ok, String message, String token, Long userId, String role) {}

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        var result = mobileAuthService.login(req.email(), req.password(), request);
        if (!result.ok()) {
            return ResponseEntity.badRequest()
                    .body(new LoginResponse(false, result.message(), null, null, null));
        }
        User u = result.user();
        return ResponseEntity.ok(new LoginResponse(true, "OK", result.token(), u.getId(), u.getRole().name()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String raw = authHeader.substring(7);
            mobileAuthService.logout(raw);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(@RequestHeader(name = "Authorization", required = false) String authHeader,
                                       HttpServletRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        String raw = authHeader.substring(7);
        var user = mobileAuthService.resolveUser(raw);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        mobileAuthService.logoutAll(user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestHeader(name = "Authorization", required = false) String authHeader,
                                                 HttpServletRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(new LoginResponse(false, "Token khong hop le.", null, null, null));
        }
        String raw = authHeader.substring(7);
        var result = mobileAuthService.refresh(raw, request);
        if (!result.ok()) {
            return ResponseEntity.badRequest()
                    .body(new LoginResponse(false, result.message(), null, null, null));
        }
        User u = result.user();
        return ResponseEntity.ok(new LoginResponse(true, "OK", result.token(), u.getId(), u.getRole().name()));
    }
}
