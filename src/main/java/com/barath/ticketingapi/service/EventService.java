package com.barath.ticketingapi.service;

import com.barath.ticketingapi.dto.EventCreateRequest;
import com.barath.ticketingapi.model.Event;
import com.barath.ticketingapi.model.Seat;
import com.barath.ticketingapi.model.SeatStatus;
import com.barath.ticketingapi.repository.EventRepository;
import com.barath.ticketingapi.repository.SeatRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;


    public EventService(EventRepository eventRepository, SeatRepository seatRepository) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
    }

    // Cached for 30 seconds to survive flash sale refreshes
    @Cacheable(value = "events")
    public List<Event> getAllEvents(){
        return eventRepository.findByEventDateAfterOrderByEventDateAsc(OffsetDateTime.now());
    }

    // NO CACHE: Real-time inventory must always hit the database.
    // USES PAGEABLE: Prevents JVM memory crashes.
    public Page<Seat> getSeatsForEvent(Long eventId, Pageable pageable){
        return seatRepository.findByEventId(eventId, pageable);
    }

    //to auto generate the seats
    @Transactional
    //to wipe the Redis cache the exact millisecond a new event is created, forcing the next user to fetch the fresh list from Postgres
    @CacheEvict(value = "events", allEntries = true)
    public Event createEvent(EventCreateRequest request) { // SECURE: Uses DTO to prevent Mass Assignment

        Event event = new Event();
        event.setName(request.name());
        event.setVenue(request.venue());
        event.setEventDate(request.eventDate());
        event.setTotalSeats(request.totalSeats());

        // Save the event first so it gets an ID from Postgres
        Event savedEvent = eventRepository.save(event);

        // Auto-generate the seats based on totalSeats
        List<Seat> seatsToSave = new ArrayList<>();
        for (int i = 1; i <= savedEvent.getTotalSeats(); i++) {
            Seat seat = new Seat();
            seat.setEventId(savedEvent.getId()); // Grab the Long ID here
            seat.setSeatNumber("Seat-" + i);
            seat.setStatus(SeatStatus.AVAILABLE);
            seatsToSave.add(seat);
        }

        // Save all the generated seats to the database in one batch
        seatRepository.saveAll(seatsToSave);

        return savedEvent;
    }

}
