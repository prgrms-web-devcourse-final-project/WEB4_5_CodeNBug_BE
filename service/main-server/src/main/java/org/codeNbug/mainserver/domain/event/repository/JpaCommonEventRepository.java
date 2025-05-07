package org.codeNbug.mainserver.domain.event.repository;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCommonEventRepository extends JpaRepository<Event, Long> {
}
