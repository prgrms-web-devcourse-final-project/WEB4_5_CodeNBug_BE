package org.codeNbug.mainserver.domain.manager.repository;

import org.codeNbug.mainserver.domain.manager.entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTypeRepository extends JpaRepository<EventType, Long> {
}
