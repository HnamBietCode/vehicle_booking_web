package com.bookvehicle.example.sr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SoberBookingScheduler {

    @Autowired
    private SoberBookingService soberBookingService;

    /**
     * Runs every 1 minute to check for sober bookings that have been pending for more than 15 minutes.
     */
    @Scheduled(fixedDelay = 60000)
    public void checkTimedOutBookings() {
        soberBookingService.autoCancelTimedOutBookings();
    }
}
