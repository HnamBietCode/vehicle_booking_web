package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.model.SoberBooking;
import com.bookvehicle.example.sr.service.SoberBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sober-bookings")
public class SoberBookingController {

    @Autowired
    private SoberBookingService soberBookingService;

    @PostMapping
    public ResponseEntity<SoberBooking> create(@RequestParam Long customerUserId, @RequestBody SoberBooking booking) {
        return ResponseEntity.ok(soberBookingService.createBooking(customerUserId, booking));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<SoberBooking>> getPending() {
        return ResponseEntity.ok(soberBookingService.getPendingBookings());
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<SoberBooking> accept(@PathVariable Long id, @RequestParam Long driverUserId) {
        try {
            return ResponseEntity.ok(soberBookingService.acceptBooking(id, driverUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{id}/arrive")
    public ResponseEntity<SoberBooking> arrive(@PathVariable Long id, @RequestParam Long driverUserId) {
        try {
            return ResponseEntity.ok(soberBookingService.arriveAtPickup(id, driverUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<SoberBooking> start(@PathVariable Long id, @RequestParam Long driverUserId) {
        try {
            return ResponseEntity.ok(soberBookingService.startTrip(id, driverUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<SoberBooking> complete(@PathVariable Long id, @RequestParam Long driverUserId) {
        try {
            return ResponseEntity.ok(soberBookingService.completeTrip(id, driverUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<SoberBooking> pay(@PathVariable Long id, @RequestParam Long customerUserId) {
        try {
            return ResponseEntity.ok(soberBookingService.payBooking(id, customerUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SoberBooking> getById(@PathVariable Long id) {
        SoberBooking booking = soberBookingService.getById(id);
        if (booking == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(booking);
    }
}
