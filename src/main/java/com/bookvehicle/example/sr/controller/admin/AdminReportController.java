package com.bookvehicle.example.sr.controller.admin;

import com.bookvehicle.example.sr.dto.ChartDataDTO;
import com.bookvehicle.example.sr.dto.ReportDashboardDTO;
import com.bookvehicle.example.sr.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<ReportDashboardDTO> getDashboardData(
            @RequestParam(name = "period", required = false, defaultValue = "monthly") String period) {
        // e.g., period = weekly | monthly | yearly
        return ResponseEntity.ok(reportService.getDashboardData(period));
    }

    @GetMapping("/charts")
    public ResponseEntity<List<ChartDataDTO>> getChartData(
            @RequestParam(name = "period", required = false, defaultValue = "monthly") String period) {
        return ResponseEntity.ok(reportService.getChartData(period));
    }
}
