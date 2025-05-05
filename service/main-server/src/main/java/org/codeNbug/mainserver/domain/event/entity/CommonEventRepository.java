package org.codeNbug.mainserver.domain.event.entity;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;

public interface CommonEventRepository {
	List<Event> findAllByFilter(EventListFilter filter);
}
