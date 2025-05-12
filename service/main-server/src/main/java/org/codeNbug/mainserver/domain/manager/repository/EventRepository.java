package org.codeNbug.mainserver.domain.manager.repository;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("SELECT e FROM Event e " +
            "LEFT JOIN FETCH e.seatLayout sl " +
            "LEFT JOIN FETCH sl.seats " +
            "WHERE e.eventId = :eventId")
    Optional<Event> findByIdWithSeats(@Param("eventId") Long eventId);

}
