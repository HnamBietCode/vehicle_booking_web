package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.LoginForm;
import com.bookvehicle.example.sr.dto.RegisterForm;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── Đăng ký ──────────────────────────────────────────────────

    /** Hiển thị form đăng ký */
    @GetMapping("/register")
    public String showRegisterForm(Model model, HttpSession session) {
        if (SecurityUtil.isLoggedIn(session))
            return "redirect:/";
        model.addAttribute("registerForm", new RegisterForm());
        model.addAttribute("loginForm", new LoginForm());
        model.addAttribute("showRegister", true);
        return "auth/login";
    }

    /** Xử lý đăng ký */
    @PostMapping("/register")
    public String handleRegister(@ModelAttribute("registerForm") RegisterForm form,
            Model model,
            RedirectAttributes redirectAttributes) {
        String error = authService.register(form);
        if (error != null) {
            model.addAttribute("errorReg", error);
            model.addAttribute("registerForm", form);
            model.addAttribute("loginForm", new LoginForm());
            model.addAttribute("showRegister", true);
            return "auth/login";
        }

        String role = form.getRole().toUpperCase();
        if ("DRIVER".equals(role)) {
            redirectAttributes.addFlashAttribute("success",
                    "Đăng ký thành công! Vui lòng chờ Admin duyệt hồ sơ trước khi đăng nhập.");
        } else {
            redirectAttributes.addFlashAttribute("success",
                    "Đăng ký thành công! Vui lòng đăng nhập.");
        }
        return "redirect:/auth/login";
    }

    // ── Đăng nhập ─────────────────────────────────────────────────

    /** Hiển thị form đăng nhập */
    @GetMapping("/login")
    public String showLoginForm(Model model,
            HttpSession session,
            @RequestParam(name = "error", required = false) String error) {
        if (SecurityUtil.isLoggedIn(session))
            return "redirect:/";
        model.addAttribute("loginForm", new LoginForm());
        model.addAttribute("registerForm", new RegisterForm());
        if ("forbidden".equals(error))
            model.addAttribute("error", "Bạn không có quyền truy cập trang đó.");
        return "auth/login";
    }

    /** Xử lý đăng nhập */
    @PostMapping("/login")
    public String handleLogin(@ModelAttribute("loginForm") LoginForm form,
            Model model,
            HttpSession session,
            @RequestParam(name = "redirect", required = false) String redirect) {
        User user = authService.login(form.getEmail(), form.getPassword());
        if (user == null) {
            model.addAttribute("loginForm", form);
            model.addAttribute("registerForm", new RegisterForm());
            model.addAttribute("error", "Email hoặc mật khẩu không đúng, hoặc tài khoản đã bị khoá.");
            return "auth/login";
        }
        SecurityUtil.setLoggedUser(session, user);

        // Redirect đến trang đích hoặc trang chủ theo role
        if (redirect != null && !redirect.isBlank())
            return "redirect:" + redirect;
        return switch (user.getRole()) {
            case ADMIN -> "redirect:/admin/users";
            case CUSTOMER -> "redirect:/profile";
            case DRIVER -> "redirect:/driver/dashboard";
        };
    }

    // ── Đăng xuất ─────────────────────────────────────────────────

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        SecurityUtil.clearSession(session);
        ra.addFlashAttribute("success", "Bạn đã đăng xuất thành công.");
        return "redirect:/auth/login";
    }

    // ── Forgot Password Flow ───────────────────────────────────────

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
        ra.addFlashAttribute("success", "Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập.");
        return "redirect:/auth/login";
    }
}
