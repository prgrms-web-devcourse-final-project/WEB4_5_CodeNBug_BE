package org.codeNbug.mainserver.domain.event.entity;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommonEventRepository {
	Page<Event> findAllByFilter(EventListFilter filter, Pageable pageable);

	Page<Event> findAllByKeyword(String keyword, Pageable pageable);

	Page<Event> findAllByFilterAndKeyword(String keyword, EventListFilter filter, Pageable pageable);

	Integer countAvailableSeat(Long id);
}
