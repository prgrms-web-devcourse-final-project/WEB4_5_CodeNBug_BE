package org.codeNbug.mainserver.domain.event.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import org.codeNbug.mainserver.domain.event.entity.CostRange;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.event.entity.EventType;
import org.codeNbug.mainserver.domain.event.entity.Location;
import org.codeNbug.mainserver.domain.event.entity.QEvent;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;

import lombok.Getter;

@Getter
public class EventListFilter {
	private CostRange costRange;
	private List<Location> locationList;
	private List<EventType> eventTypeList;
	private List<EventStatusEnum> eventStatusList;
	private LocalDateTime startDate;
	private LocalDateTime endDate;

	public EventListFilter() {
	}

	public EventListFilter(CostRange costRange, List<Location> locationList, List<EventType> eventTypeList,
		List<EventStatusEnum> eventStatusList, LocalDateTime startDate, LocalDateTime endDate) {
		this.costRange = costRange;
		this.locationList = locationList;
		this.eventTypeList = eventTypeList;
		this.eventStatusList = eventStatusList;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public boolean canFiltered() {
		return costRange != null
			|| (locationList != null && !locationList.isEmpty())
			|| (eventTypeList != null && !eventTypeList.isEmpty())
			|| (eventStatusList != null && !eventStatusList.isEmpty())
			|| startDate != null
			|| endDate != null;
	}

	public BooleanExpression getCostRangeQuery() {
		if (costRange == null) {
			return Expressions.TRUE.eq(true);
		}
		return QEvent.event.seatLayout.seats.any().grade.amount.between(costRange.getMin(), costRange.getMax());
	}

	public BooleanExpression getLocationListIncludeQuery() {
		if (locationList == null || locationList.isEmpty()) {
			return Expressions.TRUE.eq(true);
		}
		BooleanExpression expression = Expressions.FALSE;

		for (Location location : locationList) {
			expression = expression.or(
				QEvent.event.information.location.like("%" + location.getLocation() + "%"));
		}

		return expression;
	}

	public BooleanExpression getEventTypeIncludeQuery() {


		if (eventTypeList == null || eventTypeList.isEmpty()) {
			return Expressions.TRUE;
		}
		BooleanExpression expression = Expressions.FALSE;
		for (EventType eventType : eventTypeList) {
			expression = expression.or(QEvent.event.typeId.eq(eventType.getEventTypeId()));
		}
		return expression;
	}

	public BooleanExpression getEventStatusIncludeQuery() {


		if (eventStatusList == null || eventStatusList.isEmpty()) {
			return Expressions.TRUE;
		}

		BooleanExpression expression = Expressions.FALSE;
		for (EventStatusEnum eventStatus : eventStatusList) {
			expression = expression.or(QEvent.event.status.eq(eventStatus));
		}
		return expression;
	}

	public BooleanExpression getBetweenDateQuery() {
		if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
			return null;
		}

		return QEvent.event.information.eventStart.goe(startDate)
			.and(QEvent.event.information.eventEnd.loe(endDate));
	}

}
