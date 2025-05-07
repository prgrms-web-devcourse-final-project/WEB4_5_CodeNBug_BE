package org.codeNbug.mainserver.domain.event.repository;

import java.util.Optional;

import org.codeNbug.mainserver.domain.event.entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTypeRepository extends JpaRepository<EventType, Long> {
    Optional<EventType> findByName(String name);
}
