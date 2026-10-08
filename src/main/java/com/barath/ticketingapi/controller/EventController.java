package com.barath.ticketingapi.controller;

import com.barath.ticketingapi.model.Event;
import com.barath.ticketingapi.model.Seat;
import com.barath.ticketingapi.service.EventService;
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
    @GetMapping("/{eventId}/seats")
    public ResponseEntity<List<Seat>> getSeatsForEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getSeatsForEvent(eventId));
    }

    // ADMIN ONLY: Our SecurityConfig blocks normal users from hitting this POST route
    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        return ResponseEntity.ok(eventService.createEvent(event));
    }
}
