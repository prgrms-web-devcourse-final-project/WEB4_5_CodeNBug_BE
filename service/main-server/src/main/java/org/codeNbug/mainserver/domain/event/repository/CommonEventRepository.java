package org.codeNbug.mainserver.domain.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonEventRepository extends JpaRepository<Event, Long> {
}
