package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.model.Vehicle;
import com.bookvehicle.example.sr.service.VehicleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final VehicleService vehicleService;

    public HomeController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Vehicle> availableVehicles = vehicleService.searchAvailable(null, null, null, false);
        List<Vehicle> popularVehicles = availableVehicles.stream().limit(4).toList();
        model.addAttribute("popularVehicles", popularVehicles);
        return "index";
    }
}
