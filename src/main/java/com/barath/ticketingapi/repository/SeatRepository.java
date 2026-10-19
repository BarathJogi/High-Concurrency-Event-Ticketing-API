package com.barath.ticketingapi.repository;


import com.barath.ticketingapi.model.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    // 1. Pessimistic Lock path (Locks the database row immediately)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithPessimisticLock(@Param("id") Long id);

    // Prevents memory crashes by paginating the stadium seats
    Page<Seat> findByEventId(Long eventId, Pageable pageable);

    // Prevents N+1 database locks by updating all expired seats in a single SQL query
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.holdExpiresAt = null WHERE s.status = 'HELD' AND s.holdExpiresAt < :time")
    int releaseExpiredHolds(@Param("time") java.time.OffsetDateTime time);


    //List<Seat> findByStatusAndHoldExpiresAtBefore(SeatStatus status, java.time.OffsetDateTime time);
    //List<Seat> findByEventId(Long eventId);
}
