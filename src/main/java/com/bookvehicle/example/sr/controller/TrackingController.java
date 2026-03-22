package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.CustomerRepository;
import com.bookvehicle.example.sr.repository.PickupPointRepository;
import com.bookvehicle.example.sr.repository.SoberBookingRepository;
import com.bookvehicle.example.sr.repository.VehicleRentalRepository;
import com.bookvehicle.example.sr.service.GeocodingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/tracking")
public class TrackingController {

    private final CustomerRepository customerRepository;
    private final VehicleRentalRepository vehicleRentalRepository;
    private final SoberBookingRepository soberBookingRepository;
    private final PickupPointRepository pickupPointRepository;
    private final GeocodingService geocodingService;

    public TrackingController(CustomerRepository customerRepository,
                              VehicleRentalRepository vehicleRentalRepository,
                              SoberBookingRepository soberBookingRepository,
                              PickupPointRepository pickupPointRepository,
                              GeocodingService geocodingService) {
        this.customerRepository = customerRepository;
        this.vehicleRentalRepository = vehicleRentalRepository;
        this.soberBookingRepository = soberBookingRepository;
        this.pickupPointRepository = pickupPointRepository;
        this.geocodingService = geocodingService;
    }

    @GetMapping("/rental/{id}")
    public String trackRental(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.CUSTOMER) {
            ra.addFlashAttribute("error", "Chi khach hang moi duoc theo doi.");
            return "redirect:/";
        }
        Optional<Customer> customerOpt = customerRepository.findByUserId(loggedUser.getId());
        if (customerOpt.isEmpty()) {
            return "redirect:/";
        }
        Optional<VehicleRental> rentalOpt = vehicleRentalRepository.findById(id);
        if (rentalOpt.isEmpty() || !rentalOpt.get().getCustomerId().equals(customerOpt.get().getId())) {
            ra.addFlashAttribute("error", "Khong tim thay don.");
            return "redirect:/rentals/my";
        }
        model.addAttribute("tripType", "RENTAL");
        model.addAttribute("tripId", id);
        VehicleRental rental = rentalOpt.get();
        model.addAttribute("pickupAddress", rental.getPickupAddress());
        // Use stored lat/lng first (from geocoding at creation time)
        Double pickupLat = rental.getPickupLat();
        Double pickupLng = rental.getPickupLng();
        // Fallback to PickupPoint coordinates if stored lat/lng not available
        if (pickupLat == null && rental.getPickupPointId() != null) {
            pickupPointRepository.findByIdAndIsActiveTrue(rental.getPickupPointId()).ifPresent(point -> {
                if (point.getLatitude() != null && point.getLongitude() != null) {
                    model.addAttribute("pickupLat", point.getLatitude());
                    model.addAttribute("pickupLng", point.getLongitude());
                }
            });
        }
        if (pickupLat == null && rental.getPickupAddress() != null && !rental.getPickupAddress().isBlank()) {
            try {
                GeocodingService.LatLng coords = geocodingService.geocode(rental.getPickupAddress(), null, null, null);
                if (coords != null) {
                    pickupLat = coords.lat();
                    pickupLng = coords.lng();
                }
            } catch (Exception ignored) {
            }
        }
        if (pickupLat != null) {
            model.addAttribute("pickupLat", pickupLat);
            model.addAttribute("pickupLng", pickupLng);
        }
        model.addAttribute("loggedUser", loggedUser);
        return "tracking/track";
    }

    @GetMapping("/sober/{id}")
    public String trackSober(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.CUSTOMER) {
            ra.addFlashAttribute("error", "Chi khach hang moi duoc theo doi.");
            return "redirect:/";
        }
        Optional<Customer> customerOpt = customerRepository.findByUserId(loggedUser.getId());
        if (customerOpt.isEmpty()) {
            return "redirect:/";
        }
        Optional<SoberBooking> bookingOpt = soberBookingRepository.findById(id);
        if (bookingOpt.isEmpty() || !bookingOpt.get().getCustomerId().equals(customerOpt.get().getId())) {
            ra.addFlashAttribute("error", "Khong tim thay don.");
            return "redirect:/sober-bookings/my";
        }
        model.addAttribute("tripType", "SOBER");
        model.addAttribute("tripId", id);
        SoberBooking booking = bookingOpt.get();
        model.addAttribute("pickupAddress", booking.getPickupAddress());
        model.addAttribute("pickupProvince", booking.getProvince());
        model.addAttribute("pickupDistrict", booking.getDistrict());
        model.addAttribute("pickupWard", booking.getWard());
        Double pickupLat = booking.getPickupLat();
        Double pickupLng = booking.getPickupLng();
        if (pickupLat == null && booking.getPickupAddress() != null && !booking.getPickupAddress().isBlank()) {
            try {
                GeocodingService.LatLng coords = geocodingService.geocode(
                        booking.getPickupAddress(),
                        booking.getWard(),
                        booking.getDistrict(),
                        booking.getProvince()
                );
                if (coords != null) {
                    pickupLat = coords.lat();
                    pickupLng = coords.lng();
                }
            } catch (Exception ignored) {
            }
        }
        if (pickupLat != null && pickupLng != null) {
            model.addAttribute("pickupLat", pickupLat);
            model.addAttribute("pickupLng", pickupLng);
        }
        model.addAttribute("loggedUser", loggedUser);
        return "tracking/track";
    }
}
