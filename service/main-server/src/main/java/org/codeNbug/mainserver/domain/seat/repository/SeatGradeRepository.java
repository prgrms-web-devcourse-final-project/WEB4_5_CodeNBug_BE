package org.codeNbug.mainserver.domain.seat.repository;

import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {
}
