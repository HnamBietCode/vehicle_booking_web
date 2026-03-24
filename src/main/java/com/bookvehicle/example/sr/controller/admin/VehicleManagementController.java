package com.bookvehicle.example.sr.controller.admin;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.VehicleForm;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.service.FileUploadService;
import com.bookvehicle.example.sr.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.bookvehicle.example.sr.repository.RatingRepository;

@Controller
@RequestMapping("/admin/vehicles")
public class VehicleManagementController {

    private final VehicleService vehicleService;
    private final FileUploadService fileUploadService;
    private final RatingRepository ratingRepository;

    public VehicleManagementController(VehicleService vehicleService,
                                        FileUploadService fileUploadService,
                                        RatingRepository ratingRepository) {
        this.vehicleService = vehicleService;
        this.fileUploadService = fileUploadService;
        this.ratingRepository = ratingRepository;
    }

    // ── Index ────────────────────────────────────────────────────────

    @GetMapping
    public String index(@RequestParam(name = "category", required = false) String category,
                        Model model, HttpSession session) {
        List<Vehicle> vehicles;
        if (category != null && !category.isBlank()) {
            try {
                vehicles = vehicleService.findByCategory(VehicleCategory.valueOf(category.toUpperCase()));
            } catch (Exception e) {
                vehicles = vehicleService.findAll();
            }
        } else {
            vehicles = vehicleService.findAll();
        }
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "admin/vehicles/index";
    }

    // ── Detail ───────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        Optional<Vehicle> opt = vehicleService.findById(id);
        if (opt.isEmpty()) return "redirect:/admin/vehicles?error=notfound";
        Vehicle v = opt.get();
        model.addAttribute("vehicle", v);
        // Chỉ hiện tài xế có bằng lái phù hợp với loại xe
        model.addAttribute("eligibleDrivers", vehicleService.findEligibleDrivers(v.getCategory()));
        // Đánh giá xe
        model.addAttribute("vehicleRatings",
                ratingRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                        RatingTargetType.VEHICLE, v.getId()));
        // Đánh giá tài xế (nếu đã gán)
        if (v.getAssignedDriver() != null) {
            model.addAttribute("driverRatings",
                    ratingRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                            RatingTargetType.DRIVER, v.getAssignedDriver()));
        }
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "admin/vehicles/detail";
    }

    // ── Add ──────────────────────────────────────────────────────────

    @GetMapping("/add")
    public String showAddForm(Model model, HttpSession session) {
        model.addAttribute("form", new VehicleForm());
        model.addAttribute("categories", Arrays.asList(VehicleCategory.values()));
        model.addAttribute("statuses", Arrays.asList(VehicleStatus.values()));
        model.addAttribute("eligibleDrivers", vehicleService.findApprovedDrivers());
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "admin/vehicles/add";
    }

    @PostMapping("/add")
    public String handleAdd(@ModelAttribute("form") VehicleForm form,
                             Model model,
                             HttpSession session,
                             RedirectAttributes ra) {
        // Xử lý upload ảnh
        try {
            if (form.getImageFile() != null && !form.getImageFile().isEmpty()) {
                String imageUrl = fileUploadService.saveImage(form.getImageFile());
                form.setImageUrl(imageUrl);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi upload ảnh: " + e.getMessage());
            model.addAttribute("categories", Arrays.asList(VehicleCategory.values()));
            model.addAttribute("statuses", Arrays.asList(VehicleStatus.values()));
            model.addAttribute("eligibleDrivers", vehicleService.findApprovedDrivers());
            model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
            return "admin/vehicles/add";
        }

        String error = vehicleService.create(form);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("categories", Arrays.asList(VehicleCategory.values()));
            model.addAttribute("statuses", Arrays.asList(VehicleStatus.values()));
            model.addAttribute("eligibleDrivers", vehicleService.findApprovedDrivers());
            model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
            return "admin/vehicles/add";
        }
        ra.addFlashAttribute("success", "Thêm xe mới thành công.");
        return "redirect:/admin/vehicles";
    }

    // ── Edit ─────────────────────────────────────────────────────────

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session) {
        Optional<Vehicle> opt = vehicleService.findById(id);
        if (opt.isEmpty()) return "redirect:/admin/vehicles?error=notfound";
        Vehicle v = opt.get();

        VehicleForm form = new VehicleForm();
        form.setCategory(v.getCategory().name());
        form.setName(v.getName());
        form.setLicensePlate(v.getLicensePlate());
        form.setColor(v.getColor());
        form.setYear(v.getYear());
        form.setImageUrl(v.getImageUrl());
        form.setCurrentAddress(v.getCurrentAddress());
        form.setPricePerKm(v.getPricePerKm());
        form.setPricePerHour(v.getPricePerHour());
        form.setPricePerDay(v.getPricePerDay());
        form.setStatus(v.getStatus().name());
        form.setAssignedDriverId(v.getAssignedDriver());

        model.addAttribute("form", form);
        model.addAttribute("vehicle", v);
        model.addAttribute("categories", Arrays.asList(VehicleCategory.values()));
        model.addAttribute("statuses", Arrays.asList(VehicleStatus.values()));
        model.addAttribute("eligibleDrivers", vehicleService.findApprovedDrivers());
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "admin/vehicles/edit";
    }

    @PostMapping("/{id}/edit")
    public String handleEdit(@PathVariable Long id,
                              @ModelAttribute("form") VehicleForm form,
                              Model model,
                              HttpSession session,
                              RedirectAttributes ra) {
        // Xử lý upload ảnh mới
        try {
            if (form.getImageFile() != null && !form.getImageFile().isEmpty()) {
                String imageUrl = fileUploadService.saveImage(form.getImageFile());
                form.setImageUrl(imageUrl);
            }
            // Nếu không upload ảnh mới, giữ nguyên imageUrl cũ (từ hidden input)
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi upload ảnh: " + e.getMessage());
            model.addAttribute("categories", Arrays.asList(VehicleCategory.values()));
            model.addAttribute("statuses", Arrays.asList(VehicleStatus.values()));
            vehicleService.findById(id).ifPresent(v -> model.addAttribute("vehicle", v));
            model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
            return "admin/vehicles/edit";
        }

        String error = vehicleService.update(id, form);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("categories", Arrays.asList(VehicleCategory.values()));
            model.addAttribute("statuses", Arrays.asList(VehicleStatus.values()));
            vehicleService.findById(id).ifPresent(v -> model.addAttribute("vehicle", v));
            model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
            return "admin/vehicles/edit";
        }
        ra.addFlashAttribute("success", "Cập nhật xe thành công.");
        return "redirect:/admin/vehicles/" + id;
    }

    // ── Delete ───────────────────────────────────────────────────────

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        vehicleService.delete(id);
        ra.addFlashAttribute("success", "Đã xoá xe.");
        return "redirect:/admin/vehicles";
    }

    // ── Assign Driver ─────────────────────────────────────────────────

    @PostMapping("/{id}/assign-driver")
    public String assignDriver(@PathVariable Long id,
                                @RequestParam Long driverId,
                                RedirectAttributes ra) {
        String error = vehicleService.assignDriver(id, driverId);
        if (error != null) ra.addFlashAttribute("error", error);
        else ra.addFlashAttribute("success", "Đã gán tài xế thành công.");
        return "redirect:/admin/vehicles/" + id;
    }

    // ── Unassign Driver ───────────────────────────────────────────────

    @PostMapping("/{id}/unassign-driver")
    public String unassignDriver(@PathVariable Long id, RedirectAttributes ra) {
        String error = vehicleService.unassignDriver(id);
        if (error != null) ra.addFlashAttribute("error", error);
        else ra.addFlashAttribute("success", "Đã bỏ gán tài xế.");
        return "redirect:/admin/vehicles/" + id;
    }
}
