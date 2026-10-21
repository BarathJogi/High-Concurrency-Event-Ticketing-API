package com.barath.ticketingapi.controller;

import com.barath.ticketingapi.model.Booking;
import com.barath.ticketingapi.model.User;
import com.barath.ticketingapi.repository.BookingRepository;
import com.barath.ticketingapi.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    public BookingController(BookingService bookingService, BookingRepository bookingRepository) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
    }

    // Exposes the highly optimized "My Tickets" query to the frontend
    @GetMapping
    public ResponseEntity<List<Booking>> getMyBookings(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(bookingRepository.findByUserId(currentUser.getId()));
    }

    @PostMapping("/hold")
    public ResponseEntity<Booking> holdSeat(
            @RequestParam Long seatId,
            @AuthenticationPrincipal User currentUser) { // IDOR Vulnerability closed. Identity comes from JWT.

        Booking booking = bookingService.holdSeat(seatId, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
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
