package org.codeNbug.mainserver.domain.event.repository;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.entity.CommonEventRepository;
import org.codeNbug.mainserver.domain.event.entity.QEvent;
import org.codeNbug.mainserver.domain.seat.entity.QSeat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

@Repository
public class QueryDslCommonEventRepository implements CommonEventRepository {
	private final JPAQueryFactory jpaQueryFactory;

	public QueryDslCommonEventRepository(JPAQueryFactory jpaQueryFactory) {
		this.jpaQueryFactory = jpaQueryFactory;
	}

	private BooleanExpression filterDeletedFalseExpression() {
		return QEvent.event.isDeleted.isFalse();
	}

	/**
	 * {event, minPrice, maxPrice} 튜플을 리턴
	 * @param filter
	 * @param pageable
	 * @return
	 */
	@Override
	public Page<Tuple> findAllByFilter(EventListFilter filter, Pageable pageable) {

		JPAQuery<Tuple> query = jpaQueryFactory
			.select(QEvent.event,
				QSeat.seat.grade.amount.min().as("minPrice"),
				QSeat.seat.grade.amount.max().as("maxPrice"))
			.from(QEvent.event)
			.leftJoin(QSeat.seat).on(QEvent.event.eventId.eq(QSeat.seat.event.eventId))
			.where(
				Expressions.allOf(
					filter.getCostRangeQuery()
						.and(filter.getLocationListIncludeQuery())
						.and(filter.getEventCategoryIncludeQuery())
						.and(filter.getEventStatusIncludeQuery())
						.and(filter.getBetweenDateQuery())
						.and(filterDeletedFalseExpression())
				)
			)
			.groupBy(QEvent.event)
			.orderBy(QEvent.event.createdAt.desc());

		long count = query.fetchCount();
		List<Tuple> data = query.offset(pageable.getOffset())
			.limit(pageable.getPageSize()).fetch();


		return new PageImpl<>(data, pageable, count);
	}

	/**
	 * {event, minPrice, maxPrice} 튜플을 리턴
	 * @param keyword
	 * @param pageable
	 * @return
	 */
	@Override
	public Page<Tuple> findAllByKeyword(String keyword, Pageable pageable) {
		JPAQuery<Tuple> query = jpaQueryFactory.select(QEvent.event,
				QSeat.seat.grade.amount.min().as("minPrice"),
				QSeat.seat.grade.amount.max().as("maxPrice"))
			.from(QEvent.event)
			.leftJoin(QSeat.seat)
			.on(QEvent.event.eventId.eq(QSeat.seat.event.eventId))
			.where(QEvent.event.information.title.like("%" + keyword + "%")
				.and(filterDeletedFalseExpression()))
			.groupBy(QEvent.event)
			.orderBy(QEvent.event.createdAt.desc());
		long count = query.fetchCount();
		List<Tuple> data = query.offset(pageable.getOffset())
			.limit(pageable.getPageSize()).fetch();
		return new PageImpl<>(data, pageable, count);

	}

	/**
	 * {event, minPrice, maxPrice} 튜플을 리턴
	 * @param keyword
	 * @param filter
	 * @param pageable
	 * @return
	 */
	@Override
	public Page<Tuple> findAllByFilterAndKeyword(String keyword, EventListFilter filter, Pageable pageable) {
		JPAQuery<Tuple> query = jpaQueryFactory.select(QEvent.event,
				QSeat.seat.grade.amount.min().as("minPrice"),
				QSeat.seat.grade.amount.max().as("maxPrice"))
			.from(QEvent.event)
			.leftJoin(QSeat.seat)
			.on(QEvent.event.eventId.eq(QSeat.seat.event.eventId))
			.where(
				filter.getCostRangeQuery()
					.and(filter.getLocationListIncludeQuery())
					.and(filter.getEventCategoryIncludeQuery())
					.and(filter.getEventStatusIncludeQuery())
					.and(filter.getBetweenDateQuery())
					.and(QEvent.event.information.title.like("%" + keyword + "%"))
					.and(filterDeletedFalseExpression())
			)
			.groupBy(QEvent.event)
			.orderBy(QEvent.event.createdAt.desc());
		long count = query.fetchCount();
		List<Tuple> data = query.offset(pageable.getOffset())
			.limit(pageable.getPageSize()).fetch();
		return new PageImpl<>(data, pageable, count);
	}

	@Override
	public Integer countAvailableSeat(Long id) {
		return Math.toIntExact(jpaQueryFactory
			.select(QSeat.seat.count()) // 단순히 개수를 셀 것이므로 seat.count() 또는 다른 표현도 가능합니다.
			.from(QSeat.seat)
			.where(
				QSeat.seat.event.eventId.eq(id), // 기존 이벤트 ID 조건
				QSeat.seat.available.isTrue()    // available이 true인 좌석만 선택하는 조건 추가!
			)
			.fetchOne());
	}
}
