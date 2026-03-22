package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.service.SoberBookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/sober-bookings")
public class SoberBookingWebController {

    @Autowired
    private SoberBookingService soberBookingService;

    @Autowired
    private com.bookvehicle.example.sr.repository.SoberBookingRepository soberBookingRepository;

    @Autowired
    private com.bookvehicle.example.sr.repository.SoberRateRepository soberRateRepository;

    @Autowired
    private com.bookvehicle.example.sr.repository.CustomerRepository customerRepository;

    @Autowired
    private com.bookvehicle.example.sr.service.WalletService walletService;

    @GetMapping("/new")
    public String showCreateForm(HttpSession session, Model model, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.CUSTOMER) {
            ra.addFlashAttribute("error", "Chỉ khách hàng mới được thuê tài xế.");
            return "redirect:/";
        }

        model.addAttribute("booking", new SoberBooking());
        model.addAttribute("vehicleCategories", VehicleCategory.values());
        model.addAttribute("durationUnits", SoberBooking.DurationUnit.values());

        // Dynamic rates for JS estimation
        List<SoberRate> rates = soberRateRepository.findAll();
        java.util.Map<String, java.util.Map<String, java.math.BigDecimal>> rateMap = new java.util.HashMap<>();
        rateMap.put("HOURLY", new java.util.HashMap<>());
        rateMap.put("DAILY", new java.util.HashMap<>());
        for (SoberRate r : rates) {
            rateMap.get("HOURLY").put(r.getVehicleCategory().name(), r.getHourlyRate());
            rateMap.get("DAILY").put(r.getVehicleCategory().name(), r.getDailyRate());
        }
        model.addAttribute("ratesJson", rateMap);
        
        return "sober-bookings/new";
    }

    @PostMapping("/new")
    public String createBooking(@ModelAttribute SoberBooking booking, HttpSession session, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) return "redirect:/auth/login";

        try {
            soberBookingService.createBooking(loggedUser.getId(), booking);
            ra.addFlashAttribute("success", "Đặt tài xế thành công! Đang chờ tài xế nhận đơn.");
            return "redirect:/sober-bookings/my";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/sober-bookings/new";
        }
    }

    @GetMapping("/my")
    public String myBookings(HttpSession session, Model model) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) return "redirect:/auth/login";

        Customer customer = customerRepository.findByUserId(loggedUser.getId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        List<SoberBooking> bookings = soberBookingRepository.findByCustomerId(customer.getId());
        model.addAttribute("bookings", bookings);
        model.addAttribute("wallet", walletService.getOrCreateWallet(loggedUser.getId()));
        model.addAttribute("loggedUser", loggedUser);
        return "sober-bookings/my";
    }

    @PostMapping("/{id}/pay")
    public String payBooking(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) return "redirect:/auth/login";

        try {
            soberBookingService.payBooking(id, loggedUser.getId());
            ra.addFlashAttribute("success", "Thanh toán thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi thanh toán: " + e.getMessage());
        }
        return "redirect:/sober-bookings/my";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) return "redirect:/auth/login";

        try {
            soberBookingService.cancelByCustomer(id, loggedUser.getId());
            ra.addFlashAttribute("success", "Đã hủy đơn thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi hủy đơn: " + e.getMessage());
        }
        return "redirect:/sober-bookings/my";
    }
}
