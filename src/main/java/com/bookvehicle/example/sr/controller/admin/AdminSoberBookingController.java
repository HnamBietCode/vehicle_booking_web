package com.bookvehicle.example.sr.controller.admin;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.SoberBooking;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.model.SoberRate;
import com.bookvehicle.example.sr.repository.SoberRateRepository;
import com.bookvehicle.example.sr.repository.SoberBookingRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/sober-bookings")
public class AdminSoberBookingController {

    @Autowired
    private SoberBookingRepository soberBookingRepository;

    @Autowired
    private SoberRateRepository soberRateRepository;

    @GetMapping
    public String index(HttpSession session, Model model, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) return "redirect:/auth/login";
        if (loggedUser.getRole() != Role.ADMIN) {
            ra.addFlashAttribute("error", "Chỉ Admin mới được truy cập.");
            return "redirect:/";
        }

        List<SoberBooking> allBookings = soberBookingRepository.findAll();
        model.addAttribute("bookings", allBookings);
        model.addAttribute("loggedUser", loggedUser);
        
        return "admin/sober-bookings";
    }

    @GetMapping("/rates")
    public String rates(HttpSession session, Model model, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null || loggedUser.getRole() != Role.ADMIN) return "redirect:/";

        model.addAttribute("rates", soberRateRepository.findAll());
        return "admin/sober-rates";
    }

    @PostMapping("/rates/update")
    public String updateRates(@RequestParam Long rateId, 
                              @RequestParam java.math.BigDecimal hourlyRate,
                              @RequestParam java.math.BigDecimal dailyRate,
                              HttpSession session, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null || loggedUser.getRole() != Role.ADMIN) return "redirect:/";

        SoberRate rate = soberRateRepository.findById(rateId).orElse(null);
        if (rate != null) {
            rate.setHourlyRate(hourlyRate);
            rate.setDailyRate(dailyRate);
            rate.setUpdatedAt(java.time.LocalDateTime.now());
            soberRateRepository.save(rate);
            ra.addFlashAttribute("success", "Cập nhật giá thành công cho " + rate.getVehicleCategory());
        }
        return "redirect:/admin/sober-bookings/rates";
    }
}
