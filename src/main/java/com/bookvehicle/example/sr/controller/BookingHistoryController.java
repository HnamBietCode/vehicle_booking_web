package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.CustomerRepository;
import com.bookvehicle.example.sr.repository.DriverBookingRepository;
import com.bookvehicle.example.sr.repository.RatingRepository;
import com.bookvehicle.example.sr.repository.VehicleRentalRepository;
import com.bookvehicle.example.sr.service.VehicleRentalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/booking-history")
public class BookingHistoryController {

    private final CustomerRepository customerRepository;
    private final VehicleRentalRepository vehicleRentalRepository;
    private final DriverBookingRepository driverBookingRepository;
    private final VehicleRentalService vehicleRentalService;
    private final RatingRepository ratingRepository;

    public BookingHistoryController(CustomerRepository customerRepository,
                                    VehicleRentalRepository vehicleRentalRepository,
                                    DriverBookingRepository driverBookingRepository,
                                    VehicleRentalService vehicleRentalService,
                                    RatingRepository ratingRepository) {
        this.customerRepository = customerRepository;
        this.vehicleRentalRepository = vehicleRentalRepository;
        this.driverBookingRepository = driverBookingRepository;
        this.vehicleRentalService = vehicleRentalService;
        this.ratingRepository = ratingRepository;
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

        // Build set of rental IDs where driver has already been rated by this user
        Set<Long> ratedRentalIds = rentals.stream()
                .filter(r -> r.getDriverId() != null
                        && r.getStatus() == VehicleRental.RentalStatus.COMPLETED
                        && r.getPaymentStatus() == PaymentStatus.PAID)
                .filter(r -> ratingRepository.existsByReviewerIdAndTargetTypeAndTargetIdAndRefTypeAndRefId(
                        loggedUser.getId(),
                        com.bookvehicle.example.sr.model.RatingTargetType.DRIVER,
                        r.getDriverId(),
                        com.bookvehicle.example.sr.model.RatingRefType.RENTAL,
                        r.getId()))
                .map(VehicleRental::getId)
                .collect(Collectors.toSet());

        // Build set of rental IDs where vehicle has already been rated by this user
        Set<Long> ratedVehicleIds = rentals.stream()
                .filter(r -> r.getVehicleId() != null
                        && r.getStatus() == VehicleRental.RentalStatus.COMPLETED
                        && r.getPaymentStatus() == PaymentStatus.PAID)
                .filter(r -> ratingRepository.existsByReviewerIdAndTargetTypeAndTargetIdAndRefTypeAndRefId(
                        loggedUser.getId(),
                        com.bookvehicle.example.sr.model.RatingTargetType.VEHICLE,
                        r.getVehicleId(),
                        com.bookvehicle.example.sr.model.RatingRefType.RENTAL,
                        r.getId()))
                .map(VehicleRental::getId)
                .collect(Collectors.toSet());

        model.addAttribute("rentals", rentals);
        model.addAttribute("driverBookings", driverBookings);
        model.addAttribute("activeTab", tab);
        model.addAttribute("loggedUser", loggedUser);
        model.addAttribute("customer", customer);
        model.addAttribute("totalBookings", rentals.size() + driverBookings.size());
        model.addAttribute("ratedRentalIds", ratedRentalIds);
        model.addAttribute("ratedVehicleIds", ratedVehicleIds);

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
