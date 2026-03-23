package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.Vehicle;
import com.bookvehicle.example.sr.model.VehicleCategory;
import com.bookvehicle.example.sr.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/vehicles")
public class VehicleSearchController {

    private final VehicleService vehicleService;

    public VehicleSearchController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(name = "category", required = false) String category,
                         @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
                         @RequestParam(name = "location", required = false) String location,
                         @RequestParam(name = "freeDriver", required = false, defaultValue = "false") boolean freeDriver,
                         @RequestParam(name = "mode", required = false, defaultValue = "ALL") String mode,
                         Model model,
                         HttpSession session) {

        boolean requireFreeDriver = freeDriver;
        if ("WITH_DRIVER".equalsIgnoreCase(mode)) {
            requireFreeDriver = true;
        } else if ("VEHICLE_ONLY".equalsIgnoreCase(mode)) {
            requireFreeDriver = false;
        }

        // Luôn tìm kiếm - hiển thị tất cả xe (bao gồm cả xe đang ON_TRIP để hiện giao diện mờ)
        List<Vehicle> results = vehicleService.searchAll(category, maxPrice, location, requireFreeDriver);
        boolean searched = true;

        model.addAttribute("results", results);
        model.addAttribute("categories", Arrays.asList(VehicleCategory.values()));
        model.addAttribute("selectedCategory", category);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("location", location);
        model.addAttribute("freeDriver", requireFreeDriver);
        model.addAttribute("mode", mode);
        model.addAttribute("searched", searched);
        model.addAttribute("hasFilter", category != null || maxPrice != null || location != null || requireFreeDriver);
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "vehicles/search";
    }
}
