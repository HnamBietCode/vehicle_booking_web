package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.RentalCreateForm;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.PickupPointRepository;
import com.bookvehicle.example.sr.service.VehicleRentalService;
import com.bookvehicle.example.sr.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/rentals")
public class RentalController {

    private final VehicleService vehicleService;
    private final VehicleRentalService vehicleRentalService;
    private final PickupPointRepository pickupPointRepository;

    public RentalController(VehicleService vehicleService,
                            VehicleRentalService vehicleRentalService,
                            PickupPointRepository pickupPointRepository) {
        this.vehicleService = vehicleService;
        this.vehicleRentalService = vehicleRentalService;
        this.pickupPointRepository = pickupPointRepository;
    }

    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(name = "mode", required = false, defaultValue = "WITH_DRIVER") VehicleRental.RentalMode mode,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.CUSTOMER) {
            ra.addFlashAttribute("error", "Chi khach hang moi duoc dat thue xe.");
            return "redirect:/vehicles/search";
        }

        RentalCreateForm form = new RentalCreateForm();
        form.setVehicleId(vehicleId);
        form.setRentalMode(mode);
        model.addAttribute("form", form);
        populateCreateFormModel(model, loggedUser, mode);
        return "rentals/new";
    }

    @PostMapping
    public String createRental(
            @ModelAttribute("form") RentalCreateForm form,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.CUSTOMER) {
            ra.addFlashAttribute("error", "Ban khong co quyen tao don thue.");
            return "redirect:/vehicles/search";
        }

        VehicleRentalService.ServiceResult result = vehicleRentalService.createRental(loggedUser.getId(), form);
        if (!result.ok()) {
            model.addAttribute("error", result.message());
            model.addAttribute("form", form);
            populateCreateFormModel(model, loggedUser, form.getRentalMode());
            return "rentals/new";
        }

        ra.addFlashAttribute("success", result.message());
        return "redirect:/rentals/my";
    }

    @GetMapping("/my")
    public String myRentals(HttpSession session, Model model, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.CUSTOMER) {
            ra.addFlashAttribute("error", "Ban khong co quyen truy cap.");
            return "redirect:/";
        }

        List<VehicleRental> rentals = vehicleRentalService.findByCustomerUserId(loggedUser.getId());
        model.addAttribute("rentals", rentals);
        model.addAttribute("loggedUser", loggedUser);
        return "rentals/my";
    }

    @PostMapping("/{id}/pay")
    public String payRental(
            @PathVariable("id") Long rentalId,
            HttpSession session,
            RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.CUSTOMER) {
            ra.addFlashAttribute("error", "Ban khong co quyen thanh toan.");
            return "redirect:/";
        }

        VehicleRentalService.ServiceResult result = vehicleRentalService.payRental(rentalId, loggedUser.getId());
        if (result.ok()) {
            ra.addFlashAttribute("success", result.message());
        } else {
            ra.addFlashAttribute("error", result.message());
        }
        return "redirect:/rentals/my";
    }

    @PostMapping("/{id}/cancel")
    public String cancelRental(
            @PathVariable("id") Long rentalId,
            @RequestParam(name = "reason", required = false) String reason,
            HttpSession session,
            RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.CUSTOMER) {
            ra.addFlashAttribute("error", "Ban khong co quyen huy don.");
            return "redirect:/";
        }

        VehicleRentalService.ServiceResult result = vehicleRentalService.cancelRental(rentalId, loggedUser.getId(), reason);
        if (result.ok()) {
            ra.addFlashAttribute("success", result.message());
        } else {
            ra.addFlashAttribute("error", result.message());
        }
        return "redirect:/rentals/my";
    }

    private void populateCreateFormModel(Model model, User loggedUser, VehicleRental.RentalMode mode) {
        boolean requireFreeDriver = (mode == VehicleRental.RentalMode.WITH_DRIVER);
        List<Vehicle> vehicles = vehicleService.searchAvailable(null, null, null, requireFreeDriver);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("pickupPoints", pickupPointRepository.findDistinctActivePickupPoints());
        model.addAttribute("rentalTypes", VehicleRental.RentalType.values());
        model.addAttribute("rentalMode", mode);
        model.addAttribute("loggedUser", loggedUser);
    }
}
