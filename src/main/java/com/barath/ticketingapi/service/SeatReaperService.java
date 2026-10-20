package com.barath.ticketingapi.service;


import com.barath.ticketingapi.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;


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

        // Executes a single, lightning-fast SQL UPDATE query directly in Postgres.
        // Bypasses JPA entity generation entirely, saving massive amounts of JVM memory.
        int releasedCount = seatRepository.releaseExpiredHolds(now);

        if (releasedCount > 0) {
            log.info("Reaper executed: Released {} expired seat holds back to AVAILABLE", releasedCount);
        }
    }
}
