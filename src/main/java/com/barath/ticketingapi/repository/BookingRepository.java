package com.barath.ticketingapi.repository;

import com.barath.ticketingapi.model.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    // Forces a SQL JOIN to prevent the N+1 query trap on the "My Tickets" page
    @EntityGraph(attributePaths = {"seat"})
    List<Booking> findByUserId(Long userId);

    // Instantly expires abandoned checkouts directly in the database to save JVM memory
    @Modifying
    @Query("UPDATE Booking b SET b.status = 'EXPIRED' WHERE b.status = 'PENDING' AND b.createdAt < :timeoutThreshold")
    int expireUnpaidBookings(@Param("timeoutThreshold") java.time.OffsetDateTime timeoutThreshold);

}
