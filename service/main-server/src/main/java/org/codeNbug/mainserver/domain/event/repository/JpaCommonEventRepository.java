package org.codeNbug.mainserver.domain.event.repository;

import java.util.List;
import java.util.Optional;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCommonEventRepository extends JpaRepository<Event, Long> {
	Optional<Event> findByEventIdAndIsDeletedFalse(Long eventId);

	List<Event> findByIsDeletedFalse(Pageable pageable);
}
