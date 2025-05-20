package org.codeNbug.mainserver.domain.event.repository;

import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.request.EventListFilter;
import org.codeNbug.mainserver.domain.event.dto.response.EventListResponse;
import org.codeNbug.mainserver.domain.event.entity.CommonEventRepository;
import org.codeNbug.mainserver.domain.event.entity.QEvent;
import org.codeNbug.mainserver.domain.seat.entity.QSeat;
import org.codeNbug.mainserver.domain.seat.entity.QSeatGrade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
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

	private BooleanExpression filterDeletedFalseExpression(QEvent event) {
		return event.isDeleted.isFalse();
	}

	/**
	 * {event, minPrice, maxPrice} 튜플을 리턴
	 * @param filter
	 * @param pageable
	 * @return
	 */
	@Override
	public Page<EventListResponse> findAllByFilter(EventListFilter filter, Pageable pageable) {

		QEvent event = QEvent.event;
		QSeat seat = QSeat.seat;
		QSeatGrade grade = QSeatGrade.seatGrade;

		// ✅ 서브쿼리: 페이징 적용된 이벤트 ID 목록
		List<Long> eventIds = jpaQueryFactory
			.select(event.eventId)
			.from(event)
			.where(
				Expressions.allOf(
					filter.getLocationListIncludeQuery(),
					filter.getEventCategoryIncludeQuery(),
					filter.getEventStatusIncludeQuery(),
					filter.getBetweenDateQuery(),
					filterDeletedFalseExpression(event)
				)
			)
			.orderBy(event.createdAt.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		// ✅ 메인 쿼리: 조건에 맞는 event + 가격 정보 집계 포함
		List<EventListResponse> results = jpaQueryFactory
			.select(Projections.constructor(
				EventListResponse.class,
				event.eventId,
				event.category,
				event.information,
				event.bookingStart,
				event.bookingEnd,
				event.viewCount,
				event.status,
				event.seatSelectable,
				event.isDeleted,
				grade.amount.min().as("minPrice"),
				grade.amount.max().as("maxPrice")
			))
			.from(seat)
			.join(seat.grade, grade)
			.join(seat.event, event)
			.where(
				event.eventId.in(eventIds)
					.and(filter.getCostRangeQuery(seat, grade))
			)
			.groupBy(event.eventId)
			.orderBy(event.createdAt.desc()) // optional: 재정렬
			.fetch();
		// QEvent event = QEvent.event;
		// QSeat seat = QSeat.seat;
		// QSeatGrade grade = QSeatGrade.seatGrade;
		// JPAQuery<EventListResponse> query = jpaQueryFactory
		// 	.select(Projections.constructor(
		// 		EventListResponse.class,
		// 		event.eventId,
		// 		event.category,
		// 		event.information,
		// 		event.bookingStart,
		// 		event.bookingEnd,
		// 		event.viewCount,
		// 		event.status,
		// 		event.seatSelectable,
		// 		event.isDeleted,
		// 		seat.grade.amount.min().as("minPrice"),
		// 		seat.grade.amount.max().as("maxPrice")
		// 	))
		// 	.from(seat)
		// 	.limit(pageable.getPageSize())
		// 	.offset(pageable.getOffset())
		// 	.orderBy(event.createdAt.desc())
		// 	.where(
		// 		Expressions.allOf(
		// 			filter.getCostRangeQuery(seat, grade)
		// 				.and(filter.getLocationListIncludeQuery())
		// 				.and(filter.getEventCategoryIncludeQuery())
		// 				.and(filter.getEventStatusIncludeQuery())
		// 				.and(filter.getBetweenDateQuery())
		// 				.and(filterDeletedFalseExpression(event))
		// 		)
		// 	)
		// 	.groupBy(event)
		// 	.leftJoin(seat.grade, grade)
		// 	.leftJoin(seat.event, event)
		// 	;
		//
		JPAQuery<Long> countQuery = jpaQueryFactory
			.select(event.countDistinct())
			.from(seat)
			.join(seat.grade, grade)
			.join(seat.event, event)
			.where(
				Expressions.allOf(
					filter.getCostRangeQuery(seat, grade)
						.and(filter.getLocationListIncludeQuery())
						.and(filter.getEventCategoryIncludeQuery())
						.and(filter.getEventStatusIncludeQuery())
						.and(filter.getBetweenDateQuery())
						.and(filterDeletedFalseExpression(event))
				)
			);

		long count = countQuery.fetchCount();

		return new PageImpl<>(results, pageable, count);
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
				.and(filterDeletedFalseExpression(QEvent.event)))
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
				filter.getCostRangeQuery(QSeat.seat, QSeatGrade.seatGrade)
					.and(filter.getLocationListIncludeQuery())
					.and(filter.getEventCategoryIncludeQuery())
					.and(filter.getEventStatusIncludeQuery())
					.and(filter.getBetweenDateQuery())
					.and(QEvent.event.information.title.like("%" + keyword + "%"))
					.and(filterDeletedFalseExpression(QEvent.event))
			)
			.groupBy(QEvent.event)
			.orderBy(QEvent.event.createdAt.desc());
		long count = query.fetchCount();
		List<Tuple> data = query.offset(pageable.getOffset())
			.limit(pageable.getPageSize()).fetch();
		return new PageImpl<>(data, pageable, count);
	}

	@Override
	public Page<Tuple> findByIsDeletedFalse(Pageable pageable) {
		JPAQuery<Tuple> query = jpaQueryFactory.select(QEvent.event,
				QSeat.seat.grade.amount.min().as("minPrice"),
				QSeat.seat.grade.amount.max().as("maxPrice"))
			.from(QEvent.event)
			.leftJoin(QSeat.seat)
			.on(QEvent.event.eventId.eq(QSeat.seat.event.eventId))
			.where(filterDeletedFalseExpression(QEvent.event))
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
