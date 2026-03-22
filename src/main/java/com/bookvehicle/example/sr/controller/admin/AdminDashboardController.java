package com.bookvehicle.example.sr.controller.admin;

import com.bookvehicle.example.sr.config.SecurityUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    @GetMapping
    public String index(Model model, HttpSession session) {
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        model.addAttribute("pageTitle", "Báo Cáo Thống Kê");
        return "admin/dashboard";
    }
}
