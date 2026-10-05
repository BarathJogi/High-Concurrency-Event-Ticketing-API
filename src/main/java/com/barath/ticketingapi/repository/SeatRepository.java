package com.barath.ticketingapi.repository;

import com.barath.ticketingapi.model.Seat;
import com.barath.ticketingapi.model.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    // 1. Pessimistic Lock path (Locks the database row immediately)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithPessimisticLock(@Param("id") Long id);

    List<Seat> findByStatusAndHoldExpiresAtBefore(SeatStatus status, java.time.OffsetDateTime time);

}
