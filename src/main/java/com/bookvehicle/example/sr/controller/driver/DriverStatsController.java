package com.bookvehicle.example.sr.controller.driver;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.ChartDataDTO;
import com.bookvehicle.example.sr.dto.ReportDashboardDTO;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.service.DriverReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import com.bookvehicle.example.sr.repository.DriverRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/driver/stats")
public class DriverStatsController {

    @Autowired
    private DriverReportService driverReportService;

    @Autowired
    private DriverRepository driverRepository;

    @GetMapping
    public String showStats(
            @RequestParam(name = "period", defaultValue = "monthly") String period,
            HttpSession session, 
            Model model, 
            RedirectAttributes ra) {
        
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.DRIVER) {
            ra.addFlashAttribute("error", "Chỉ tài xế mới được truy cập trang này.");
            return "redirect:/";
        }

        if (driverRepository.findByUserId(loggedUser.getId()).isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy hồ sơ tài xế của bạn. Vui lòng liên hệ Admin.");
            return "redirect:/profile";
        }

        ReportDashboardDTO dashboard = driverReportService.getDashboardData(loggedUser.getId(), period);
        List<ChartDataDTO> chartData = driverReportService.getChartData(loggedUser.getId(), period);

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("chartData", chartData);
        model.addAttribute("period", period);
        model.addAttribute("loggedUser", loggedUser);

        return "driver/stats";
    }
}
