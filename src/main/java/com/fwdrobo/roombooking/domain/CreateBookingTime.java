package com.fwdrobo.roombooking.domain;

import java.time.LocalDateTime;

public record CreateBookingTime(
        LocalDateTime start,
        LocalDateTime end
) {
}
