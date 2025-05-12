package org.codeNbug.mainserver.domain.event.repository;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.entity.CommonEventRepository;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.QEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

@Repository
public class QueryDslCommonEventRepository implements CommonEventRepository {
	private final JPAQueryFactory jpaQueryFactory;

	public QueryDslCommonEventRepository(JPAQueryFactory jpaQueryFactory) {
		this.jpaQueryFactory = jpaQueryFactory;
	}

	@Override
	public Page<Event> findAllByFilter(EventListFilter filter, Pageable pageable) {
		JPAQuery<Event> query = jpaQueryFactory.selectFrom(QEvent.event)
			.where(
				Expressions.allOf(
					filter.getCostRangeQuery()
						.and(filter.getLocationListIncludeQuery())
						.and(filter.getEventCategoryIncludeQuery())
						.and(filter.getEventStatusIncludeQuery())
						.and(filter.getBetweenDateQuery())
				)
			)
			.orderBy(QEvent.event.createdAt.desc());
		long count = query.fetchCount();
		List<Event> data = query.offset(pageable.getOffset())
			.limit(pageable.getPageSize()).fetch();

		return new PageImpl<>(data, pageable, count);
	}

	@Override
	public Page<Event> findAllByKeyword(String keyword, Pageable pageable) {
		JPAQuery<Event> query = jpaQueryFactory.selectFrom(QEvent.event)
			.where(QEvent.event.information.title.like("%" + keyword + "%"))
			.orderBy(QEvent.event.createdAt.desc());
		long count = query.fetchCount();
		List<Event> data = query.offset(pageable.getOffset())
			.limit(pageable.getPageSize()).fetch();
		return new PageImpl<>(data, pageable, count);

	}

	@Override
	public Page<Event> findAllByFilterAndKeyword(String keyword, EventListFilter filter, Pageable pageable) {
		JPAQuery<Event> query = jpaQueryFactory.selectFrom(QEvent.event)
			.where(
				filter.getCostRangeQuery()
					.and(filter.getLocationListIncludeQuery())
					.and(filter.getEventCategoryIncludeQuery())
					.and(filter.getEventStatusIncludeQuery())
					.and(filter.getBetweenDateQuery())
					.and(QEvent.event.information.title.like("%" + keyword + "%"))
			);
		long count = query.fetchCount();
		List<Event> data = query.offset(pageable.getOffset())
			.limit(pageable.getPageSize()).fetch();
		return new PageImpl<>(data, pageable, count);
	}

	@Override
	public Integer countAvailableSeat(Long id) {
		return jpaQueryFactory
			.select(QEvent.event.seatLayout.seats.size())
			.from(QEvent.event)
			.where(QEvent.event.eventId.eq(id).and(QEvent.event.seatLayout.seats.any().available.isTrue()))
			.fetchOne();
	}
}
