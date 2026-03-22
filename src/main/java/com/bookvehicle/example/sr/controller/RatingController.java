package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.RatingForm;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.DriverRepository;
import com.bookvehicle.example.sr.repository.VehicleRepository;
import com.bookvehicle.example.sr.service.RatingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/ratings")
public class RatingController {

    private final RatingService ratingService;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public RatingController(RatingService ratingService,
                            DriverRepository driverRepository,
                            VehicleRepository vehicleRepository) {
        this.ratingService = ratingService;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // ── Form đánh giá ───────────────────────────────────────────────

    @GetMapping("/rate")
    public String showRateForm(@RequestParam(name = "targetType") String targetType,
                                @RequestParam(name = "targetId") Long targetId,
                                @RequestParam(name = "refType", required = false, defaultValue = "OTHER") String refType,
                                @RequestParam(name = "refId", required = false, defaultValue = "0") Long refId,
                                Model model, HttpSession session) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) return "redirect:/auth/login";

        if (loggedUser.getRole().name().equals("DRIVER")) {
            Driver d = driverRepository.findByUserId(loggedUser.getId()).orElse(null);
            if (d != null) {
                if ("DRIVER".equalsIgnoreCase(targetType) && d.getId().equals(targetId)) {
                    return "redirect:/ratings/driver/" + targetId + "?error=true";
                }
                if ("VEHICLE".equalsIgnoreCase(targetType)) {
                    Vehicle v = vehicleRepository.findById(targetId).orElse(null);
                    if (v != null && d.getId().equals(v.getAssignedDriver())) {
                        return "redirect:/ratings/vehicle/" + targetId + "?error=true";
                    }
                }
            }
        }

        RatingForm form = new RatingForm();
        form.setTargetType(targetType);
        form.setTargetId(targetId);
        form.setRefType(refType);
        form.setRefId(refId);

        model.addAttribute("form", form);

        // Load tên target để hiển thị trên form
        if ("DRIVER".equalsIgnoreCase(targetType)) {
            driverRepository.findById(targetId).ifPresent(d ->
                    model.addAttribute("targetName", d.getFullName()));
        } else if ("VEHICLE".equalsIgnoreCase(targetType)) {
            vehicleRepository.findById(targetId).ifPresent(v -> {
                model.addAttribute("targetName", v.getName() + " (" + v.getLicensePlate() + ")");
                if (v.getAssignedDriver() != null) {
                    driverRepository.findById(v.getAssignedDriver()).ifPresent(d -> 
                        model.addAttribute("assignedDriver", d)
                    );
                }
            });
        }

        model.addAttribute("loggedUser", loggedUser);
        return "ratings/rate";
    }

    @PostMapping("/rate")
    public String handleRate(@ModelAttribute("form") RatingForm form,
                              HttpSession session,
                              RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) return "redirect:/auth/login";

        if (loggedUser.getRole().name().equals("DRIVER")) {
            Driver d = driverRepository.findByUserId(loggedUser.getId()).orElse(null);
            if (d != null) {
                if ("DRIVER".equalsIgnoreCase(form.getTargetType()) && d.getId().equals(form.getTargetId())) {
                    ra.addFlashAttribute("error", "Tài xế không thể tự đánh giá chính mình.");
                    return "redirect:/ratings/driver/" + form.getTargetId();
                }
                if ("VEHICLE".equalsIgnoreCase(form.getTargetType())) {
                    Vehicle v = vehicleRepository.findById(form.getTargetId()).orElse(null);
                    if (v != null && d.getId().equals(v.getAssignedDriver())) {
                        ra.addFlashAttribute("error", "Tài xế không thể tự đánh giá xe mình đang lái.");
                        return "redirect:/ratings/vehicle/" + form.getTargetId();
                    }
                }
            }
        }

        String error = ratingService.rate(loggedUser.getId(), form);
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return "redirect:/ratings/rate?targetType=" + form.getTargetType()
                    + "&targetId=" + form.getTargetId()
                    + "&refType=" + form.getRefType()
                    + "&refId=" + form.getRefId();
        }

        // Xử lý lưu thêm đánh giá cho tài xế nếu đánh giá form kép từ trang đánh giá xe
        if ("VEHICLE".equalsIgnoreCase(form.getTargetType()) && form.getDriverStars() != null) {
            Vehicle v = vehicleRepository.findById(form.getTargetId()).orElse(null);
            if (v != null && v.getAssignedDriver() != null) {
                RatingForm driverForm = new RatingForm();
                driverForm.setTargetType("DRIVER");
                driverForm.setTargetId(v.getAssignedDriver());
                driverForm.setStars(form.getDriverStars());
                driverForm.setComment(form.getDriverComment());
                driverForm.setRefType(form.getRefType());
                driverForm.setRefId(form.getRefId());
                ratingService.rate(loggedUser.getId(), driverForm);
            }
        }

        ra.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá!");

        // Quay về trang đánh giá tài xế hoặc xe
        if ("DRIVER".equalsIgnoreCase(form.getTargetType())) {
            return "redirect:/ratings/driver/" + form.getTargetId();
        }
        return "redirect:/ratings/vehicle/" + form.getTargetId();
    }

    // ── Xem đánh giá tài xế ─────────────────────────────────────────

    @GetMapping("/driver/{driverId}")
    public String driverRatings(@PathVariable Long driverId,
                                @RequestParam(name = "error", required = false) String error,
                                Model model, HttpSession session) {
        List<Rating> ratings = ratingService.findDriverRatings(driverId);
        Double avg = ratingService.getAvgRating(RatingTargetType.DRIVER, driverId);
        driverRepository.findById(driverId).ifPresent(d -> model.addAttribute("driver", d));
        model.addAttribute("ratings", ratings);
        model.addAttribute("avgRating", avg);
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        if (error != null && error.equals("true")) {
            model.addAttribute("error", "Tài xế không thể tự đánh giá chính mình.");
        }
        return "ratings/driver-ratings";
    }

    // ── Xem đánh giá xe ─────────────────────────────────────────────

    @GetMapping("/vehicle/{vehicleId}")
    public String vehicleRatings(@PathVariable Long vehicleId, 
                                 @RequestParam(name = "error", required = false) String error, 
                                 Model model, HttpSession session) {
        List<Rating> ratings = ratingService.findVehicleRatings(vehicleId);
        Double avg = ratingService.getAvgRating(RatingTargetType.VEHICLE, vehicleId);
        vehicleRepository.findById(vehicleId).ifPresent(v -> model.addAttribute("vehicle", v));
        model.addAttribute("ratings", ratings);
        model.addAttribute("avgRating", avg);
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        if (error != null && error.equals("true")) {
            model.addAttribute("error", "Tài xế không thể tự đánh giá xe mình đang lái.");
        }
        return "ratings/vehicle-ratings";
    }
}
