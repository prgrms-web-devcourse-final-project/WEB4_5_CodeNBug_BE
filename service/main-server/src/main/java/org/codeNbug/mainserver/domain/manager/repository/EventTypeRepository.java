package org.codeNbug.mainserver.domain.manager.repository;

import org.codeNbug.mainserver.domain.manager.entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventTypeRepository extends JpaRepository<EventType, Long> {
    Optional<EventType> findByName(String name);
}
