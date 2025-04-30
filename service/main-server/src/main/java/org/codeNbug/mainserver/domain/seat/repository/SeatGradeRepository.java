package org.codeNbug.mainserver.domain.seat.repository;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {
    List<SeatGrade> findByEvent(Event event);
}
