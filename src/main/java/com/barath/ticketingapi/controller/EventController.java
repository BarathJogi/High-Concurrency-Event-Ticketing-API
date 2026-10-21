package com.barath.ticketingapi.controller;

import com.barath.ticketingapi.dto.EventCreateRequest;
import com.barath.ticketingapi.model.Event;
import com.barath.ticketingapi.model.Seat;
import com.barath.ticketingapi.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // PUBLIC: Anyone can browse events
    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    // PUBLIC: Anyone can check seat availability
    // FIXED: Requires Pageable to prevent OutOfMemory crashes on large venues
    @GetMapping("/{eventId}/seats")
    public ResponseEntity<Page<Seat>> getSeatsForEvent(@PathVariable Long eventId, @PageableDefault(size = 100) Pageable pageable) {
        return ResponseEntity.ok(eventService.getSeatsForEvent(eventId, pageable));
    }

    // ADMIN ONLY: Our SecurityConfig blocks normal users from hitting this POST route
    // FIXED: Uses DTO to prevent Mass Assignment, @Valid for data integrity, 201 CREATED status
    @PostMapping
    public ResponseEntity<Event> createEvent(@Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }
}
