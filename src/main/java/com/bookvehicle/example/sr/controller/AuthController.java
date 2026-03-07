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
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    /** Xử lý đăng ký */
    @PostMapping("/register")
    public String handleRegister(@ModelAttribute("form") RegisterForm form,
            Model model,
            RedirectAttributes redirectAttributes) {
        String error = authService.register(form);
        if (error != null) {
            model.addAttribute("error", error);
            return "auth/register";
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
            @RequestParam(required = false) String error) {
        if (SecurityUtil.isLoggedIn(session))
            return "redirect:/";
        model.addAttribute("form", new LoginForm());
        if ("forbidden".equals(error))
            model.addAttribute("error", "Bạn không có quyền truy cập trang đó.");
        return "auth/login";
    }

    /** Xử lý đăng nhập */
    @PostMapping("/login")
    public String handleLogin(@ModelAttribute("form") LoginForm form,
            Model model,
            HttpSession session,
            @RequestParam(required = false) String redirect) {
        User user = authService.login(form.getEmail(), form.getPassword());
        if (user == null) {
            model.addAttribute("form", form);
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
            case DRIVER -> "redirect:/profile";
        };
    }

    // ── Đăng xuất ─────────────────────────────────────────────────

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        SecurityUtil.clearSession(session);
        ra.addFlashAttribute("success", "Bạn đã đăng xuất thành công.");
        return "redirect:/auth/login";
    }
}
