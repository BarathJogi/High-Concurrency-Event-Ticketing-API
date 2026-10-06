package com.barath.ticketingapi.controller;

import com.barath.ticketingapi.model.Booking;
import com.barath.ticketingapi.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/hold")
    public ResponseEntity<Booking> holdSeat(@RequestParam Long seatId, @RequestParam Long userId){

        Booking booking = bookingService.holdSeat(seatId, userId );
        return ResponseEntity.ok(booking);

    }

    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<Booking> confirmBooking(@PathVariable Long bookingId, @RequestHeader("Idempotency-Key") String idempotencyKey) {

        Booking booking = bookingService.confirmBooking(bookingId, idempotencyKey);
        return ResponseEntity.ok(booking);

    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Booking> cancelBooking(@PathVariable Long bookingId) {

        Booking booking = bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok(booking);

    }
}
