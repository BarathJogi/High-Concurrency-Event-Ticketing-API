package com.barath.ticketingapi.dto;

import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

public record EventCreateRequest(@NotBlank
                                 String name,
                                 @NotBlank
                                 String venue,
                                 @NotNull
                                 @Future
                                 OffsetDateTime eventDate,
                                 @NotNull
                                 @Min(1)
                                 @Max(10000)
                                 Integer totalSeats) {
}
