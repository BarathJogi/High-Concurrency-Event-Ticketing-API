package com.barath.ticketingapi.repository;

import com.barath.ticketingapi.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    //Optional<Event> findById(long id);

}
