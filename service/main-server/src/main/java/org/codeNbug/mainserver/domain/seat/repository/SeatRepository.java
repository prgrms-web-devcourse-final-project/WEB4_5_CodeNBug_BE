package org.codeNbug.mainserver.domain.seat.repository;

import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
}
