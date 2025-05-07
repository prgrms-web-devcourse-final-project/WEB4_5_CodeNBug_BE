package org.codeNbug.mainserver.domain.event.repository;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

}
