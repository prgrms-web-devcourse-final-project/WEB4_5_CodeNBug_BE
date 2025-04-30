package org.codeNbug.mainserver.domain.seat.repository;

import java.util.Optional;

import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatLayoutRepository extends JpaRepository<SeatLayout, Long> {
	Optional<SeatLayout> findByEvent_EventId(Long eventId);
}
