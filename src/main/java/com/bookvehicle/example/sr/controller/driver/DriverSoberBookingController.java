package com.bookvehicle.example.sr.controller.driver;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.dto.ReportDashboardDTO;
import com.bookvehicle.example.sr.repository.DriverRepository;
import com.bookvehicle.example.sr.repository.SoberBookingRepository;
import com.bookvehicle.example.sr.service.DriverReportService;
import com.bookvehicle.example.sr.service.SoberBookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/driver/sober-bookings")
public class DriverSoberBookingController {

    @Autowired
    private SoberBookingService soberBookingService;

    @Autowired
    private SoberBookingRepository soberBookingRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private DriverReportService driverReportService;

    @GetMapping
    public String listBookings(HttpSession session, Model model, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) return "redirect:/auth/login";
        if (loggedUser.getRole() != Role.DRIVER) {
            ra.addFlashAttribute("error", "Chỉ tài xế mới được truy cập.");
            return "redirect:/";
        }

        Optional<Driver> driverOpt = driverRepository.findByUserId(loggedUser.getId());
        if (driverOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy hồ sơ tài xế của bạn. Vui lòng liên hệ Admin.");
            return "redirect:/profile";
        }
        Driver driver = driverOpt.get();

        List<SoberBooking> pendingBookings = soberBookingRepository.findPendingBookings();
        List<SoberBooking> myBookings = soberBookingRepository.findByDriverId(driver.getId());
        List<SoberBooking> historyBookings = soberBookingRepository.findDriverHistory(driver.getId());

        // Add daily stats for the dashboard header
        ReportDashboardDTO todayStats = driverReportService.getDashboardData(loggedUser.getId(), "daily");

        model.addAttribute("pendingBookings", pendingBookings);
        model.addAttribute("myBookings", myBookings);
        model.addAttribute("historyBookings", historyBookings);
        model.addAttribute("todayStats", todayStats);
        model.addAttribute("driver", driver);
        model.addAttribute("loggedUser", loggedUser);
        
        return "driver/sober-bookings";
    }

    @PostMapping("/{id}/accept")
    public String accept(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        try {
            soberBookingService.acceptBooking(id, loggedUser.getId());
            ra.addFlashAttribute("success", "Đã nhận đơn thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/driver/sober-bookings";
    }

    @PostMapping("/{id}/arrive")
    public String arrive(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        try {
            soberBookingService.arriveAtPickup(id, loggedUser.getId());
            ra.addFlashAttribute("success", "Đã xác nhận đến điểm đón!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/driver/sober-bookings";
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        try {
            soberBookingService.startTrip(id, loggedUser.getId());
            ra.addFlashAttribute("success", "Bắt đầu chuyến đi!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/driver/sober-bookings";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        try {
            soberBookingService.completeTrip(id, loggedUser.getId());
            ra.addFlashAttribute("success", "Hoàn thành chuyến đi!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/driver/sober-bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam String reason, HttpSession session, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        try {
            soberBookingService.cancelByDriver(id, loggedUser.getId(), reason);
            ra.addFlashAttribute("success", "Đã hủy đơn hàng thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/driver/sober-bookings";
    }

    @PostMapping("/toggle-status")
    public String toggleStatus(HttpSession session, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        try {
            Driver driver = driverRepository.findByUserId(loggedUser.getId())
                    .orElseThrow(() -> new RuntimeException("Driver not found"));
            
            // Check if driver has active bookings before allowing to go offline
            if (Boolean.TRUE.equals(driver.getIsAvailable())) {
                // Going Offline
                driver.setIsAvailable(false);
                ra.addFlashAttribute("success", "Bạn đã Chế độ NGOẠI TUYẾN (OFFLINE).");
            } else {
                // Going Online - only if no active bookings (though logic should handle this)
                // If they are BUSY because of a trip, they can't manually go "Online" until finished.
                boolean hasActive = soberBookingRepository.findByDriverId(driver.getId()).stream()
                        .anyMatch(b -> b.getStatus() != SoberBooking.SoberBookingStatus.COMPLETED && 
                                       b.getStatus() != SoberBooking.SoberBookingStatus.CANCELLED);
                
                if (hasActive) {
                    throw new RuntimeException("Bạn không thể chuyển sang trạng thái Rảnh khi đang thực hiện chuyến đi.");
                }
                
                driver.setIsAvailable(true);
                ra.addFlashAttribute("success", "Bạn đã Chế độ TRỰC TUYẾN (ONLINE).");
            }
            
            driverRepository.save(driver);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/driver/sober-bookings";
    }

    @PostMapping("/location")
    public String updateLocation(@RequestParam String province, 
                                 @RequestParam String district, 
                                 @RequestParam String ward, 
                                 HttpSession session, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        try {
            Driver driver = driverRepository.findByUserId(loggedUser.getId())
                    .orElseThrow(() -> new RuntimeException("Driver not found"));
            
            driver.setProvince(province);
            driver.setDistrict(district);
            driver.setWard(ward);
            driverRepository.save(driver);
            
            ra.addFlashAttribute("success", "Cập nhật vị trí hiện tại thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/driver/sober-bookings";
    }
}
