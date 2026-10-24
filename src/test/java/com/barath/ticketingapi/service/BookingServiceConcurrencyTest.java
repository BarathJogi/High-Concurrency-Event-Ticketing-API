package com.barath.ticketingapi.service;

import com.barath.ticketingapi.model.*;
import com.barath.ticketingapi.repository.BookingRepository;
import com.barath.ticketingapi.repository.EventRepository;
import com.barath.ticketingapi.repository.SeatRepository;
import com.barath.ticketingapi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.barath.ticketingapi.TestcontainersConfiguration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class BookingServiceConcurrencyTest {


    @Autowired
    private BookingService bookingService;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private BookingRepository bookingRepository;

    @AfterEach
    void tearDown() {
        // Prevents test bleeding by wiping the DB after execution
        bookingRepository.deleteAll();
        seatRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testConcurrentSeatHold() throws InterruptedException{
        // Create dummy data in the real PostgreSQL test database
        User user = new User();
        user.setName("testuser");
        user.setEmail("test@test.com");
        user.setPasswordHash("dummy_password_hash_123");
        user.setRole(UserRole.USER);
        user = userRepository.save(user);

        Event event = new Event();
        event.setName("Champions League Final");
        event.setVenue("Wembley Stadium");
        event.setEventDate(java.time.OffsetDateTime.now().plusDays(30));
        event.setTotalSeats(50000);
        event = eventRepository.save(event);

        Seat seat = new Seat();
        seat.setEventId(event.getId());
        seat.setSeatNumber("A1");
        seat.setStatus(SeatStatus.AVAILABLE);
        seat = seatRepository.save(seat);

        Long targetSeatId = seat.getId();
        Long targetUserId = user.getId();


        // CONCURRENCY ENGINE: Set up 500 concurrent threads
        int threadCount = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);

        AtomicInteger successfulHolds = new AtomicInteger(0);
        AtomicInteger failedHolds = new AtomicInteger(0);

        // EXECUTE: Load all 500 threads at the starting line
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGun.await(); // Every thread pauses here waiting for the countdown

                    bookingService.holdSeat(targetSeatId, targetUserId);
                    successfulHolds.incrementAndGet(); // If it succeeds without an exception

                } catch (Exception e) {
                    failedHolds.incrementAndGet(); // Redis rejection or OptimisticLockException
                } finally {
                    finishLine.countDown();
                }
            });
        }

        // FIRE! Release the latch and let all 500 hit the database instantly
        startGun.countDown();

        // Wait for all 500 threads to finish processing
        finishLine.await();

        executor.shutdown();

        // ASSERT: The mathematical proof that your locks work
        assertEquals(1, successfulHolds.get(), "Only ONE request should ever succeed");
        assertEquals(499, failedHolds.get(), "Exactly 499 requests must be blocked and fail");

        // Verify the database state remains pure
        Seat updatedSeat = seatRepository.findById(targetSeatId).orElseThrow();
        assertEquals(SeatStatus.HELD, updatedSeat.getStatus(), "Seat status should be HELD");
        assertEquals(1, bookingRepository.count(), "Only one booking record should exist");

    }
}
