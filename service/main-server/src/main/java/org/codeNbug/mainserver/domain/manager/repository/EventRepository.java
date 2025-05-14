package org.codeNbug.mainserver.domain.manager.repository;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("SELECT e FROM Event e " +
            "LEFT JOIN FETCH e.seatLayout sl " +
            "LEFT JOIN FETCH sl.seats " +
            "WHERE e.eventId = :eventId")
    Optional<Event> findByIdWithSeats(@Param("eventId") Long eventId);

    /**
     * 삭제된 모든 이벤트를 조회합니다.
     * 
     * @return 삭제된 이벤트 목록
     */
    List<Event> findAllByIsDeletedTrue();

    /**
     * 삭제되지 않은 모든 이벤트를 조회합니다.
     * 
     * @return 삭제되지 않은 이벤트 목록
     */
    List<Event> findAllByIsDeletedFalse();
}
