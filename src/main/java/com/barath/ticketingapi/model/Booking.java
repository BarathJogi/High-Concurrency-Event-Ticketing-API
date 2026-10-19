package com.barath.ticketingapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bookings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"idempotency_key"})
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Column(name = "user_id", nullable = false)
//    private Long userId;
//
//    @Column(name = "seat_id", nullable = false)
//    private Long seatId;

    // Changed from Long to User, and used @ManyToOne mapping
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // Changed from Long to Seat, and used @ManyToOne mapping
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    @JsonIgnore
    private Seat seat;


    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @NotBlank
    @Size(max = 255)
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;


}
