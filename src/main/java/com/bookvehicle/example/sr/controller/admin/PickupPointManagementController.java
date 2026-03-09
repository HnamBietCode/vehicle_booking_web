package com.bookvehicle.example.sr.controller.admin;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.PickupPointForm;
import com.bookvehicle.example.sr.model.PickupPoint;
import com.bookvehicle.example.sr.service.PickupPointService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/pickup-points")
public class PickupPointManagementController {

    private final PickupPointService pickupPointService;

    public PickupPointManagementController(PickupPointService pickupPointService) {
        this.pickupPointService = pickupPointService;
    }

    @GetMapping
    public String index(Model model, HttpSession session) {
        List<PickupPoint> points = pickupPointService.findAll();
        model.addAttribute("points", points);
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "admin/pickup-points/index";
    }

    @GetMapping("/add")
    public String showAddForm(Model model, HttpSession session) {
        model.addAttribute("form", new PickupPointForm());
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "admin/pickup-points/add";
    }

    @PostMapping("/add")
    public String handleAdd(@ModelAttribute("form") PickupPointForm form,
                            Model model,
                            HttpSession session,
                            RedirectAttributes ra) {
        String error = pickupPointService.create(form);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
            return "admin/pickup-points/add";
        }
        ra.addFlashAttribute("success", "Them diem don thanh cong.");
        return "redirect:/admin/pickup-points";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session) {
        Optional<PickupPoint> opt = pickupPointService.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/admin/pickup-points?error=notfound";
        }

        PickupPoint p = opt.get();
        PickupPointForm form = new PickupPointForm();
        form.setName(p.getName());
        form.setAddress(p.getAddress());
        form.setLatitude(p.getLatitude());
        form.setLongitude(p.getLongitude());
        form.setIsActive(p.getIsActive());

        model.addAttribute("point", p);
        model.addAttribute("form", form);
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "admin/pickup-points/edit";
    }

    @PostMapping("/{id}/edit")
    public String handleEdit(@PathVariable Long id,
                             @ModelAttribute("form") PickupPointForm form,
                             Model model,
                             HttpSession session,
                             RedirectAttributes ra) {
        String error = pickupPointService.update(id, form);
        if (error != null) {
            model.addAttribute("error", error);
            pickupPointService.findById(id).ifPresent(p -> model.addAttribute("point", p));
            model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
            return "admin/pickup-points/edit";
        }
        ra.addFlashAttribute("success", "Cap nhat diem don thanh cong.");
        return "redirect:/admin/pickup-points";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        String error = pickupPointService.deactivate(id);
        if (error != null) {
            ra.addFlashAttribute("error", error);
        } else {
            ra.addFlashAttribute("success", "Da an diem don.");
        }
        return "redirect:/admin/pickup-points";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes ra) {
        String error = pickupPointService.activate(id);
        if (error != null) {
            ra.addFlashAttribute("error", error);
        } else {
            ra.addFlashAttribute("success", "Da kich hoat lai diem don.");
        }
        return "redirect:/admin/pickup-points";
    }
}
