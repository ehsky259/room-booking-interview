package com.fwdrobo.roombooking.api;

import java.net.URI;
import java.time.LocalDateTime;

import com.fwdrobo.roombooking.domain.Booking;
import com.fwdrobo.roombooking.domain.CreateBookingTime;
import com.fwdrobo.roombooking.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms/{roomId}")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/bookings/{bookingId}")
    public BookingResponse getBooking(
            @PathVariable String roomId,
            @PathVariable String bookingId
    ) {
        return BookingResponse.from(bookingService.get(roomId, bookingId));
    }

    @GetMapping("/availability")
    public AvailabilityResponse getAvailability(
            @PathVariable String roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return new AvailabilityResponse(bookingService.isAvailable(roomId, start, end));
    }

    @PostMapping("/bookings")
    public ResponseEntity<Booking> createBooking(
            @PathVariable String roomId,
            @RequestBody CreateBookingTime bookingTime
    ) {
        Booking booking = bookingService.create(roomId, bookingTime.start(), bookingTime.end());
        URI location = URI.create("/rooms/" + roomId + "/bookings/" + booking.id());
        return ResponseEntity
                .created(location)
                .body(booking);
    }
}
