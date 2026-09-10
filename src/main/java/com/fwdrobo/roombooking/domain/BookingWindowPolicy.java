package com.fwdrobo.roombooking.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class BookingWindowPolicy {

    public BookingWindowResult evaluate(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return BookingWindowResult.MISSING_BOUNDARY;
        }
        if (!start.isBefore(end)) {
            return BookingWindowResult.END_NOT_AFTER_START;
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes > 120 || minutes < 30) {
            return BookingWindowResult.DURATION_OUT_OF_RANGE;
        }
        return BookingWindowResult.VALID;
    }
}