package org.codeNbug.mainserver.domain.seat.repository;

import java.util.List;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {
	List<SeatGrade> findByEvent(Event event);
}
