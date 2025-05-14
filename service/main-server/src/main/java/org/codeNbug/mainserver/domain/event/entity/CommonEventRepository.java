package org.codeNbug.mainserver.domain.event.entity;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.querydsl.core.Tuple;

public interface CommonEventRepository {
	Page<Tuple> findAllByFilter(EventListFilter filter, Pageable pageable);

	Page<Tuple> findAllByKeyword(String keyword, Pageable pageable);

	Page<Tuple> findAllByFilterAndKeyword(String keyword, EventListFilter filter, Pageable pageable);

	Integer countAvailableSeat(Long id);
}
