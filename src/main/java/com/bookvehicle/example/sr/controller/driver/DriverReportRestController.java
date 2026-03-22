package com.bookvehicle.example.sr.controller.driver;

import com.bookvehicle.example.sr.dto.ChartDataDTO;
import com.bookvehicle.example.sr.dto.ReportDashboardDTO;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.service.DriverReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/driver/reports")
public class DriverReportRestController {

    @Autowired
    private DriverReportService driverReportService;

    @GetMapping("/dashboard")
    public ResponseEntity<ReportDashboardDTO> getDashboardData(
            @RequestParam(name = "period", defaultValue = "monthly") String period,
            HttpSession session) {

        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            return ResponseEntity.status(401).build();
        }

        ReportDashboardDTO data = driverReportService.getDashboardData(loggedUser.getId(), period);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/charts")
    public ResponseEntity<List<ChartDataDTO>> getChartData(
            @RequestParam(name = "period", defaultValue = "monthly") String period,
            HttpSession session) {

        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            return ResponseEntity.status(401).build();
        }

        List<ChartDataDTO> data = driverReportService.getChartData(loggedUser.getId(), period);
        return ResponseEntity.ok(data);
    }
}
