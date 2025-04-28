package org.codeNbug.mainserver.domain.manager.repository;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

}
