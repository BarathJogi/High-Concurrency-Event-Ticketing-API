package com.barath.ticketingapi.service;

import com.barath.ticketingapi.model.*;
import com.barath.ticketingapi.repository.BookingRepository;
import com.barath.ticketingapi.repository.SeatRepository;
import com.barath.ticketingapi.repository.UserRepository;
import lombok.Getter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;


@Service
public class BookingService {
    private final RedisLockService redisLockService;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public BookingService(RedisLockService redisLockService, SeatRepository seatRepository,
                          UserRepository userRepository, BookingRepository bookingRepository ) {
        this.redisLockService = redisLockService;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    //@Transactional ensures this entire method is one atomic database operation.
    @Transactional
    public Booking holdSeat(Long seatId, Long userId){

        // This stops 499 out of 500 concurrent requests instantly without hitting Postgres.
        boolean acquired = redisLockService.acquireHold(seatId, userId);

        if(!acquired){
            throw new RuntimeException("Seat is already being held by another user.");
        }


        try {

            // Fetch the Seat. We use standard findById which relies on the @Version
            // field in the Seat entity to handle Optimistic Locking.
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new RuntimeException("Seat not found"));

            // Double-check the database truth. Even if Redis let them in
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new RuntimeException("Seat is not available.");
            }

            // Verify the user actually exists.
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update the seat status
            seat.setStatus(SeatStatus.HELD);
            seat.setHoldExpiresAt(java.time.OffsetDateTime.now().plusMinutes(10));

            // Create the booking record
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setSeat(seat);
            booking.setStatus(BookingStatus.PENDING);
            booking.setCreatedAt(java.time.OffsetDateTime.now());

            // Save the pending booking to the database
            return bookingRepository.save(booking);

        } catch (Exception e) {

            // GHOST-LOCK PREVENTION:
            // If the Postgres transaction fails (e.g., OptimisticLockException from
            // a split-second race condition), we MUST release the Redis lock manually
            // so the seat isn't stuck for 10 minutes
            redisLockService.releaseHold(seatId);
            throw e;
        }
    }
}
