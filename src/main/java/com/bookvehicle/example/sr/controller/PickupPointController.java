package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.repository.PickupPointRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pickup-points")
public class PickupPointController {

    private final PickupPointRepository pickupPointRepository;

    public PickupPointController(PickupPointRepository pickupPointRepository) {
        this.pickupPointRepository = pickupPointRepository;
    }

    @GetMapping
    public String index(Model model, HttpSession session) {
        model.addAttribute("points", pickupPointRepository.findDistinctActivePickupPoints());
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "pickup-points/index";
    }
}
