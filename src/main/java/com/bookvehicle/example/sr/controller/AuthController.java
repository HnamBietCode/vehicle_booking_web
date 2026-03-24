package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.LoginForm;
import com.bookvehicle.example.sr.dto.RegisterForm;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.service.AuthService;
import com.bookvehicle.example.sr.service.GoogleAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    @Value("${app.google.client-id}")
    private String googleClientId;

    public AuthController(AuthService authService, GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model, HttpSession session) {
        if (SecurityUtil.isLoggedIn(session)) {
            return "redirect:/";
        }
        model.addAttribute("registerForm", new RegisterForm());
        model.addAttribute("loginForm", new LoginForm());
        model.addAttribute("googleClientId", googleClientId);
        model.addAttribute("showRegister", true);
        return "auth/login";
    }

    @PostMapping("/register")
    public String handleRegister(@ModelAttribute("registerForm") RegisterForm form,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        AuthService.RegistrationResult result = authService.register(form);
        if (!result.ok()) {
            model.addAttribute("errorReg", result.message());
            model.addAttribute("registerForm", form);
            model.addAttribute("loginForm", new LoginForm());
            model.addAttribute("googleClientId", googleClientId);
            model.addAttribute("showRegister", true);
            return "auth/login";
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "Dang ky thanh cong. Ma OTP da duoc gui den Gmail cua ban."
        );
        return "redirect:/auth/register/verify?email=" + encode(result.email());
    }

    @GetMapping("/register/verify")
    public String showRegisterVerifyPage(@RequestParam("email") String email,
                                         Model model,
                                         HttpSession session,
                                         RedirectAttributes ra) {
        if (SecurityUtil.isLoggedIn(session)) {
            return "redirect:/";
        }

        Optional<User> userOpt = authService.findByEmail(email);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Khong tim thay tai khoan can xac thuc.");
            return "redirect:/auth/login";
        }

        User user = userOpt.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            if (user.getRole().name().equals("DRIVER")) {
                ra.addFlashAttribute("success", "Email da duoc xac thuc. Ho so tai xe cua ban dang cho Admin duyet.");
            } else {
                ra.addFlashAttribute("success", "Email nay da duoc xac thuc. Vui long dang nhap.");
            }
            return "redirect:/auth/login";
        }

        model.addAttribute("email", user.getEmail());
        model.addAttribute("isDriver", user.getRole().name().equals("DRIVER"));
        return "auth/verify-registration";
    }

    @PostMapping("/register/verify")
    public String verifyRegistrationOtp(@RequestParam("email") String email,
                                        @RequestParam("otp") String otp,
                                        Model model,
                                        RedirectAttributes ra) {
        Optional<User> userOpt = authService.findByEmail(email);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Khong tim thay tai khoan can xac thuc.");
            return "redirect:/auth/login";
        }

        User user = userOpt.get();
        String error = authService.verifyRegistrationOtp(email, otp);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("email", user.getEmail());
            model.addAttribute("otp", otp);
            model.addAttribute("isDriver", user.getRole().name().equals("DRIVER"));
            return "auth/verify-registration";
        }

        if (user.getRole().name().equals("DRIVER")) {
            ra.addFlashAttribute("success", "Xac thuc email thanh cong. Ho so tai xe dang cho Admin duyet.");
        } else {
            ra.addFlashAttribute("success", "Xac thuc email thanh cong. Ban co the dang nhap ngay.");
        }
        return "redirect:/auth/login";
    }

    @PostMapping("/register/resend-otp")
    public String resendRegistrationOtp(@RequestParam("email") String email,
                                        RedirectAttributes ra) {
        String error = authService.resendRegistrationOtp(email);
        if (error != null) {
            ra.addFlashAttribute("error", error);
        } else {
            ra.addFlashAttribute("success", "Ma OTP moi da duoc gui lai den Gmail cua ban.");
        }
        return "redirect:/auth/register/verify?email=" + encode(email.trim().toLowerCase());
    }

    @GetMapping("/login")
    public String showLoginForm(Model model,
                                HttpSession session,
                                @RequestParam(name = "error", required = false) String error) {
        if (SecurityUtil.isLoggedIn(session)) {
            return "redirect:/";
        }
        model.addAttribute("loginForm", new LoginForm());
        model.addAttribute("registerForm", new RegisterForm());
        model.addAttribute("googleClientId", googleClientId);
        if ("forbidden".equals(error)) {
            model.addAttribute("error", "Ban khong co quyen truy cap trang do.");
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String handleLogin(@ModelAttribute("loginForm") LoginForm form,
                              Model model,
                              HttpSession session,
                              @RequestParam(name = "redirect", required = false) String redirect) {
        AuthService.LoginResult result = authService.attemptLogin(form.getEmail(), form.getPassword());
        if (!result.ok()) {
            model.addAttribute("loginForm", form);
            model.addAttribute("registerForm", new RegisterForm());
            model.addAttribute("googleClientId", googleClientId);
            model.addAttribute("error", result.message());
            return "auth/login";
        }

        User user = result.user();
        SecurityUtil.setLoggedUser(session, user);
        if (redirect != null && !redirect.isBlank()) {
            return "redirect:" + redirect;
        }
        return switch (user.getRole()) {
            case ADMIN -> "redirect:/admin/users";
            case CUSTOMER -> "redirect:/profile";
            case DRIVER -> "redirect:/driver/dashboard";
        };
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        SecurityUtil.clearSession(session);
        ra.addFlashAttribute("success", "Ban da dang xuat thanh cong.");
        return "redirect:/auth/login";
    }

    @PostMapping("/google")
    @ResponseBody
    public Map<String, Object> handleGoogleLogin(@RequestBody Map<String, String> body,
                                                 HttpSession session) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return Map.of("success", false, "error", "Token khong hop le.");
        }
        User user = googleAuthService.authenticateWithGoogle(idToken);
        if (user == null) {
            return Map.of("success", false, "error", "Xac thuc Google that bai.");
        }
        SecurityUtil.setLoggedUser(session, user);
        String redirectUrl = switch (user.getRole()) {
            case ADMIN -> "/admin/users";
            case CUSTOMER -> "/profile";
            case DRIVER -> "/driver/dashboard";
        };
        return Map.of("success", true, "redirect", redirectUrl);
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm(Model model) {
        model.addAttribute("step", 1);
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password/send-otp")
    public String sendOtp(@RequestParam("email") String email, Model model) {
        String error = authService.sendPasswordResetOtp(email);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("step", 1);
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }
        model.addAttribute("step", 2);
        model.addAttribute("email", email);
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password/verify-otp")
    public String verifyOtp(@RequestParam("email") String email,
                            @RequestParam("otp") String otp,
                            Model model) {
        String error = authService.verifyPasswordResetOtp(email, otp);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("step", 2);
            model.addAttribute("email", email);
            model.addAttribute("otp", otp);
            return "auth/forgot-password";
        }
        model.addAttribute("step", 3);
        model.addAttribute("email", email);
        model.addAttribute("otp", otp);
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password/reset")
    public String resetPassword(@RequestParam("email") String email,
                                @RequestParam("otp") String otp,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                RedirectAttributes ra,
                                Model model) {
        String error = authService.resetPassword(email, otp, newPassword, confirmPassword);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("step", 3);
            model.addAttribute("email", email);
            model.addAttribute("otp", otp);
            return "auth/forgot-password";
        }
        ra.addFlashAttribute("success", "Mat khau da duoc dat lai thanh cong. Vui long dang nhap.");
        return "redirect:/auth/login";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
