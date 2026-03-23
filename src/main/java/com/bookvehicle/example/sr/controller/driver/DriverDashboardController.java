package com.bookvehicle.example.sr.controller.driver;

import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/driver")
public class DriverDashboardController {

    @GetMapping("/dashboard")
    public String showDriverDashboard(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập.");
            return "redirect:/auth/login";
        }

        if (loggedUser.getRole() != Role.DRIVER) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập trang này.");
            return "redirect:/"; // redirect to home or some error page
        }

        model.addAttribute("loggedUser", loggedUser);

        return "driver/dashboard";
    }
}
