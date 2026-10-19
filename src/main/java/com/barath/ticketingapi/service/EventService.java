package com.barath.ticketingapi.service;

import com.barath.ticketingapi.model.Event;
import com.barath.ticketingapi.model.Seat;
import com.barath.ticketingapi.model.SeatStatus;
import com.barath.ticketingapi.repository.EventRepository;
import com.barath.ticketingapi.repository.SeatRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    // Cached individually per event ID
    @Cacheable(value = "eventSeats", key = "#eventId")
    public List<Seat> getSeatsForEvent(Long eventId){
        return seatRepository.findByEventId(eventId);
    }

    //to auto generate the seats
    @Transactional
    //to wipe the Redis cache the exact millisecond a new event is created, forcing the next user to fetch the fresh list from Postgres
    @CacheEvict(value = "events", allEntries = true)
    public Event createEvent(Event event) {
        // 1. Save the event first so it gets an ID from Postgres
        Event savedEvent = eventRepository.save(event);

        // 2. Auto-generate the seats based on totalSeats
        List<Seat> seatsToSave = new ArrayList<>();
        for (int i = 1; i <= savedEvent.getTotalSeats(); i++) {
            Seat seat = new Seat();
            seat.setEventId(savedEvent.getId()); // Grab the Long ID here
            seat.setSeatNumber("Seat-" + i);
            seat.setStatus(SeatStatus.AVAILABLE);
            seatsToSave.add(seat);
        }

        // 3. Save all the generated seats to the database in one batch
        seatRepository.saveAll(seatsToSave);

        return savedEvent;
    }

}
