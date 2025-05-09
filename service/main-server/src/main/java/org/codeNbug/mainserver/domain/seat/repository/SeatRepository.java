package org.codeNbug.mainserver.domain.seat.repository;

import java.util.List;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {
	@Query("SELECT s FROM Seat s JOIN FETCH s.grade WHERE s.layout.id = :layoutId ORDER BY s.location ASC")
	List<Seat> findAllByLayoutIdWithGrade(@Param("layoutId") Long layoutId);

	List<Seat> findByEvent(Event event);

	@Query("SELECT s FROM Seat s WHERE s.event.eventId = :eventId AND s.available = true ORDER BY s.location ASC")
	List<Seat> findAvailableSeatsByEventId(@Param("eventId") Long eventId);

	List<Seat> findByTicketId(Long ticketId);

	@Query("SELECT s FROM Seat s WHERE s.event.eventId = :eventId AND s.available = true ORDER BY s.id ASC")
	List<Seat> findFirstByEventIdAndAvailableTrue(@Param("eventId") Long eventId);
}
