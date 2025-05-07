package org.codeNbug.mainserver.domain.event.repository;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.entity.CommonEventRepository;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

@Repository
public class QueryDslCommonEventRepository implements CommonEventRepository {
	private final JPAQueryFactory jpaQueryFactory;

	public QueryDslCommonEventRepository(JPAQueryFactory jpaQueryFactory) {
		this.jpaQueryFactory = jpaQueryFactory;
	}

	@Override
	public List<Event> findAllByFilter(EventListFilter filter) {
		// jpaQueryFactory.selectFrom(QEvent.event)
		// 	.where()
		return null;
	}
}
