package org.codeNbug.mainserver.domain.seat.repository;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeatLayoutRepository extends JpaRepository<SeatLayout, Long> {
    Optional<SeatLayout> findByEvent(Event event);
}
