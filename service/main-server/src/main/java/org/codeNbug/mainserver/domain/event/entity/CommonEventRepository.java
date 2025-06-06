package org.codeNbug.mainserver.domain.event.entity;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.querydsl.core.Tuple;

public interface CommonEventRepository {
	Page<EventListResponse> findAllByFilter(EventListFilter filter, Pageable pageable);

	Page<EventListResponse> findAllByKeyword(String keyword, Pageable pageable);

	Page<EventListResponse> findAllByFilterAndKeyword(String keyword, EventListFilter filter, Pageable pageable);

	Integer countAvailableSeat(Long id);

	Page<Tuple> findByIsDeletedFalse(Pageable pageable);
}
