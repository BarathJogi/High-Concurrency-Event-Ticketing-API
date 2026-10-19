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
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 200)
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank
    @Column(name = "venue", nullable = false)
    private String venue;

    @Future
    @Column(name = "event_date", nullable = false)
    private OffsetDateTime eventDate;

    @Min(1)
    @Max(10000)
    @NotNull
    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
