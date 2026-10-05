package com.barath.ticketingapi.service;


import com.barath.ticketingapi.model.Seat;
import com.barath.ticketingapi.model.SeatStatus;
import com.barath.ticketingapi.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SeatReaperService {

    private static final Logger log = LoggerFactory.getLogger(SeatReaperService.class);
    private final SeatRepository seatRepository;

    public SeatReaperService(SeatRepository seatRepository){
        this.seatRepository = seatRepository;
    }

    // Wakes up every 60 seconds (60000 milliseconds)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredHolds(){
        OffsetDateTime now = OffsetDateTime.now();

        // Ask the database for all seats that are HELD where the expiration time has already passed
        List<Seat> expiredSeats = seatRepository.findByStatusAndHoldExpiresAtBefore(SeatStatus.HELD, now);

        // If no seats are expired, just stop here. No need to do any work!
        if (expiredSeats.isEmpty()) {
            return;
        }

        // Loop through the list of expired seats we got from the database
        for (Seat seat : expiredSeats) {
            // Reset them to AVAILABLE and clear the timer
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHoldExpiresAt(null);
        }

        seatRepository.saveAll(expiredSeats);

        log.info("Reaper executed: Released {} expired seat holds back to AVAILABLE", expiredSeats.size());
    }
}
