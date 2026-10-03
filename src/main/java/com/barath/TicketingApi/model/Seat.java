package com.barath.TicketingApi.model;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(name = "event_id", nullable = false)
    private Long eventId; // camelCase

    @Column(name = "seat_number", nullable = false)
    private String seatNumber; // VARCHAR -> String

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "hold_expires_at")
    private OffsetDateTime holdExpiresAt; // Timezone-aware timestamp

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
