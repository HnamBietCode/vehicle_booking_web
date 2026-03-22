package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.dto.BookingRealtimeEvent;
import com.bookvehicle.example.sr.model.SoberBooking;
import com.bookvehicle.example.sr.model.VehicleRental;
import com.bookvehicle.example.sr.repository.SoberBookingRepository;
import com.bookvehicle.example.sr.repository.VehicleRentalRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookingRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SoberBookingRepository soberBookingRepository;
    private final VehicleRentalRepository vehicleRentalRepository;

    public BookingRealtimeService(SimpMessagingTemplate messagingTemplate,
                                  SoberBookingRepository soberBookingRepository,
                                  VehicleRentalRepository vehicleRentalRepository) {
        this.messagingTemplate = messagingTemplate;
        this.soberBookingRepository = soberBookingRepository;
        this.vehicleRentalRepository = vehicleRentalRepository;
    }

    public void publishSoberUpdate(SoberBooking booking, String eventType, String message) {
        if (booking == null || booking.getId() == null) {
            return;
        }
        BookingRealtimeEvent event = new BookingRealtimeEvent();
        event.setTripType("SOBER");
        event.setTripId(booking.getId());
        event.setStatus(booking.getStatus() != null ? booking.getStatus().name() : null);
        event.setEventType(eventType);
        event.setMessage(message);
        event.setTrackingUrl("/tracking/sober/" + booking.getId());
        event.setLocationAvailable(false);
        messagingTemplate.convertAndSend(topic("SOBER", booking.getId()), event);
    }

    public void publishRentalUpdate(VehicleRental rental, String eventType, String message) {
        if (rental == null || rental.getId() == null) {
            return;
        }
        BookingRealtimeEvent event = new BookingRealtimeEvent();
        event.setTripType("RENTAL");
        event.setTripId(rental.getId());
        event.setStatus(rental.getStatus() != null ? rental.getStatus().name() : null);
        event.setEventType(eventType);
        event.setMessage(message);
        event.setTrackingUrl("/tracking/rental/" + rental.getId());
        event.setLocationAvailable(false);
        messagingTemplate.convertAndSend(topic("RENTAL", rental.getId()), event);
    }

    public void publishLocationAvailable(String tripType, Long tripId) {
        if (tripType == null || tripId == null) {
            return;
        }

        String normalized = tripType.toUpperCase();
        BookingRealtimeEvent event = new BookingRealtimeEvent();
        event.setTripType(normalized);
        event.setTripId(tripId);
        event.setEventType("LOCATION_AVAILABLE");
        event.setMessage("Tai xe da khoi hanh va dang chia se vi tri. Ban co the theo doi tren ban do.");
        event.setLocationAvailable(true);

        if ("SOBER".equals(normalized)) {
            soberBookingRepository.findById(tripId).ifPresent(booking -> {
                event.setStatus(booking.getStatus() != null ? booking.getStatus().name() : null);
                event.setTrackingUrl("/tracking/sober/" + tripId);
                messagingTemplate.convertAndSend(topic(normalized, tripId), event);
            });
            return;
        }

        if ("RENTAL".equals(normalized)) {
            vehicleRentalRepository.findById(tripId).ifPresent(rental -> {
                event.setStatus(rental.getStatus() != null ? rental.getStatus().name() : null);
                event.setTrackingUrl("/tracking/rental/" + tripId);
                messagingTemplate.convertAndSend(topic(normalized, tripId), event);
            });
        }
    }

    private String topic(String tripType, Long tripId) {
        return "/topic/bookings/" + tripType + "/" + tripId;
    }
}
