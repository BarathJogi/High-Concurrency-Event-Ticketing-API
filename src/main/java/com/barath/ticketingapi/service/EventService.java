package com.barath.ticketingapi.service;

import com.barath.ticketingapi.model.Event;
import com.barath.ticketingapi.model.Seat;
import com.barath.ticketingapi.repository.EventRepository;
import com.barath.ticketingapi.repository.SeatRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
        return eventRepository.findAll();
    }

    // Cached individually per event ID
    @Cacheable(value = "eventSeats", key = "#eventId")
    public List<Seat> getSeatsForEvent(Long eventId){
        return seatRepository.findByEventId(eventId);
    }

    // No cache here, this is a write operation
    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }

}
