package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.CustomerRepository;
import com.bookvehicle.example.sr.repository.DriverBookingRepository;
import com.bookvehicle.example.sr.repository.VehicleRentalRepository;
import com.bookvehicle.example.sr.service.VehicleRentalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/booking-history")
public class BookingHistoryController {

    private final CustomerRepository customerRepository;
    private final VehicleRentalRepository vehicleRentalRepository;
    private final DriverBookingRepository driverBookingRepository;
    private final VehicleRentalService vehicleRentalService;

    public BookingHistoryController(CustomerRepository customerRepository,
                                    VehicleRentalRepository vehicleRentalRepository,
                                    DriverBookingRepository driverBookingRepository,
                                    VehicleRentalService vehicleRentalService) {
        this.customerRepository = customerRepository;
        this.vehicleRentalRepository = vehicleRentalRepository;
        this.driverBookingRepository = driverBookingRepository;
        this.vehicleRentalService = vehicleRentalService;
    }

    @GetMapping
    public String bookingHistory(
            @RequestParam(name = "tab", required = false, defaultValue = "all") String tab,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.CUSTOMER) {
            ra.addFlashAttribute("error", "Chỉ khách hàng mới xem được lịch sử đặt xe.");
            return "redirect:/";
        }

        Customer customer = customerRepository.findByUserId(loggedUser.getId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Long customerId = customer.getId();

        List<VehicleRental> rentals = vehicleRentalRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        List<DriverBooking> driverBookings = driverBookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);

        model.addAttribute("rentals", rentals);
        model.addAttribute("driverBookings", driverBookings);
        model.addAttribute("activeTab", tab);
        model.addAttribute("loggedUser", loggedUser);
        model.addAttribute("customer", customer);
        model.addAttribute("totalBookings", rentals.size() + driverBookings.size());

        return "bookings/history";
    }

    @PostMapping("/{id}/cancel")
    public String cancelRental(@PathVariable("id") Long rentalId,
                               HttpSession session,
                               RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) return "redirect:/auth/login";

        VehicleRentalService.ServiceResult result = vehicleRentalService.cancelByCustomer(rentalId, loggedUser.getId());
        if (result.ok()) {
            ra.addFlashAttribute("success", result.message());
        } else {
            ra.addFlashAttribute("error", result.message());
        }
        return "redirect:/booking-history";
    }

    @PostMapping("/{id}/pay")
    public String payRental(@PathVariable("id") Long rentalId,
                            HttpSession session,
                            RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) return "redirect:/auth/login";

        VehicleRentalService.ServiceResult result = vehicleRentalService.payRental(rentalId, loggedUser.getId());
        if (result.ok()) {
            ra.addFlashAttribute("success", result.message());
        } else {
            ra.addFlashAttribute("error", result.message());
        }
        return "redirect:/booking-history";
    }
}
