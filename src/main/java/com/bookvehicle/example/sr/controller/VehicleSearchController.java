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
                         Model model,
                         HttpSession session) {

        // Luôn tìm kiếm - mặc định hiển thị tất cả xe khả dụng
        List<Vehicle> results = vehicleService.searchAvailable(category, maxPrice, location, freeDriver);
        boolean searched = true;

        model.addAttribute("results", results);
        model.addAttribute("categories", Arrays.asList(VehicleCategory.values()));
        model.addAttribute("selectedCategory", category);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("location", location);
        model.addAttribute("freeDriver", freeDriver);
        model.addAttribute("searched", searched);
        model.addAttribute("hasFilter", category != null || maxPrice != null || location != null || freeDriver);
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "vehicles/search";
    }
}
