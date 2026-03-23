package com.bookvehicle.example.sr.controller.admin;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.SoberBookingDTO;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.SoberBooking;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.repository.SoberBookingRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sober-bookings")
public class AdminSoberBookingRestController {

    @Autowired
    private SoberBookingRepository soberBookingRepository;

    @GetMapping
    public ResponseEntity<Page<SoberBookingDTO>> getBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session, HttpServletRequest request) {

        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(401).build();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<SoberBooking> bookings = soberBookingRepository.findAll(pageable);

        return ResponseEntity.ok(bookings.map(this::convertToDTO));
    }

    private SoberBookingDTO convertToDTO(SoberBooking b) {
        SoberBookingDTO dto = new SoberBookingDTO();
        dto.setId(b.getId());
        dto.setPickupAddress(b.getPickupAddress());
        dto.setTotalPrice(b.getTotalPrice());
        dto.setStatus(b.getStatus().name());
        dto.setPaymentStatus(b.getPaymentStatus().name());
        dto.setCreatedAt(b.getCreatedAt());

        if (b.getCustomer() != null) {
            dto.setCustomerName(b.getCustomer().getFullName());
            if (b.getCustomer().getUser() != null) {
                dto.setCustomerPhone(b.getCustomer().getUser().getPhone());
            }
        } else {
            dto.setCustomerName("N/A");
        }

        if (b.getDriver() != null) {
            dto.setDriverName(b.getDriver().getFullName());
            if (b.getDriver().getUser() != null) {
                dto.setDriverPhone(b.getDriver().getUser().getPhone());
            }
        } else {
            dto.setDriverName("—");
        }

        return dto;
    }
}
