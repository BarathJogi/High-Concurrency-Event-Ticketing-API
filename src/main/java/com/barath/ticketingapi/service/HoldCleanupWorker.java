package com.barath.ticketingapi.service;

import com.barath.ticketingapi.model.Booking;
import com.barath.ticketingapi.model.BookingStatus;
import com.barath.ticketingapi.model.Seat;
import com.barath.ticketingapi.model.SeatStatus;
import com.barath.ticketingapi.repository.BookingRepository;
import com.barath.ticketingapi.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class HoldCleanupWorker {

    private static final Logger logger = LoggerFactory.getLogger(HoldCleanupWorker.class);
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;

    public HoldCleanupWorker(BookingRepository bookingRepository, SeatRepository seatRepository) {
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
    }

    // Runs every 60,000 milliseconds (1 minute)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredHolds() {
        // Define the TTL: 15 minutes ago
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusMinutes(15);

        List<Booking> expiredBookings = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING, cutoffTime);

        if (!expiredBookings.isEmpty()) {
            logger.info("Found {} expired seat holds. Releasing back to available pool...", expiredBookings.size());

            for (Booking booking : expiredBookings) {
                // 1. Mark the booking as cancelled/failed
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);

                // 2. Release the lock on the seat
                Seat seat = seatRepository.findById(booking.getSeat().getId()).orElseThrow();
                seat.setStatus(SeatStatus.AVAILABLE);
                seatRepository.save(seat);

                logger.info("Released seat ID: {} from expired booking ID: {}", seat.getId(), booking.getId());
            }
        }
    }
}
