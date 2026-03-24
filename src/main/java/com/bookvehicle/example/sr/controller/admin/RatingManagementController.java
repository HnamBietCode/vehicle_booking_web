package com.bookvehicle.example.sr.controller.admin;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.RatingRepository;
import com.bookvehicle.example.sr.repository.DriverRepository;
import com.bookvehicle.example.sr.repository.VehicleRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/ratings")
public class RatingManagementController {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @GetMapping
    public String index(@RequestParam(name = "tab", required = false, defaultValue = "driver") String tab,
                        Model model, HttpSession session) {
        List<Rating> driverRatings = ratingRepository.findByTargetTypeOrderByCreatedAtDesc(
                RatingTargetType.DRIVER);
        List<Rating> vehicleRatings = ratingRepository.findByTargetTypeOrderByCreatedAtDesc(
                RatingTargetType.VEHICLE);
        model.addAttribute("driverRatings", driverRatings);
        model.addAttribute("vehicleRatings", vehicleRatings);
        model.addAttribute("drivers", driverRepository.findAll());
        model.addAttribute("vehicles", vehicleRepository.findAll());
        model.addAttribute("tab", tab);
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "admin/ratings/index";
    }

    @PostMapping("/{id}/delete")
    public String deleteRating(@PathVariable Long id, RedirectAttributes ra) {
        ratingRepository.deleteById(id);
        ra.addFlashAttribute("success", "Đã xoá đánh giá.");
        return "redirect:/admin/ratings";
    }
}
