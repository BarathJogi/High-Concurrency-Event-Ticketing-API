package com.barath.ticketingapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "seat_number"})
})
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // BIGINT -> Long

    @NotNull
    @Positive
    @Column(name = "event_id", nullable = false)
    private Long eventId; // camelCase

    @NotBlank
    @Size(max = 25)
    @Column(name = "seat_number", nullable = false)
    private String seatNumber; // VARCHAR -> String

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @FutureOrPresent
    @Column(name = "hold_expires_at")
    private OffsetDateTime holdExpiresAt; // Timezone-aware timestamp

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
