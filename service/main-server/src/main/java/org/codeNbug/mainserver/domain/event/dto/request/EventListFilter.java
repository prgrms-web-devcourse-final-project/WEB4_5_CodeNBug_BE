package org.codeNbug.mainserver.domain.event.dto.request;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codeNbug.mainserver.domain.event.entity.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import lombok.Getter;

@Getter
public class EventListFilter {
	private CostRange costRange;
	private List<Location> locationList;
	private List<EventCategoryEnum> eventCategoryList;
	private List<EventStatusEnum> eventStatusList;
	private LocalDateTime startDate;
	private LocalDateTime endDate;

	private EventListFilter(Builder builder) {
		this.costRange = builder.costRange;
		this.locationList = builder.locationList;
		this.eventCategoryList = builder.eventCategoryList;
		this.eventStatusList = builder.eventStatusList;
		this.startDate = builder.startDate;
		this.endDate = builder.endDate;
	}

	protected EventListFilter() {
	}

	public boolean canFiltered() {
		return costRange != null
				|| (locationList != null && !locationList.isEmpty())
				|| (eventCategoryList != null && !eventCategoryList.isEmpty()
				|| (eventStatusList != null && !eventStatusList.isEmpty())
				|| startDate != null
				|| endDate != null);
	}

	@JsonIgnore
	public BooleanExpression getCostRangeQuery() {
		if (costRange == null) {
			return Expressions.TRUE.eq(true);
		}
		return QEvent.event.seatLayout.seats.any().grade.amount.between(costRange.getMin(), costRange.getMax());
	}

	/**
	 * Location 리스트 안의 location의 string을 LIKE %String% 을 이용해 필터링합니다
	 * @return
	 */
	@JsonIgnore
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

    @JsonIgnore
	public BooleanExpression getEventCategoryIncludeQuery() {
		if (eventCategoryList == null || eventCategoryList.isEmpty()) {
			return Expressions.TRUE;
		}
		BooleanExpression expression = Expressions.FALSE;
		for (EventCategoryEnum category : eventCategoryList) {
			expression = expression.or(QEvent.event.category.eq(category));
		}
		return expression;
	}

	@JsonIgnore
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

	/**
	 * 필터의 시작일이 예매 종료일보다 이전에 있거나
	 * 필터의 종료일이 예매 시작일보다 앞에 있는 행사를 조회
	 */
	@JsonIgnore
	public BooleanExpression getBetweenDateQuery() {
		if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
			return null;
		}

		return QEvent.event.bookingStart.before(endDate).and(
			QEvent.event.bookingEnd.after(startDate)
		);

	}

	public static class Builder {
		private CostRange costRange;
		private List<Location> locationList;
		private List<EventCategoryEnum> eventCategoryList;
		private List<EventStatusEnum> eventStatusList;
		private LocalDateTime startDate;
		private LocalDateTime endDate;

		@JsonIgnore
		public Builder costRange(CostRange costRange) {
			this.costRange = costRange;
			return this;
		}

		@JsonIgnore
		public Builder locationList(List<Location> locationList) {
			this.locationList = locationList;
			return this;
		}

		@JsonIgnore
		public Builder eventCategoryList(List<EventCategoryEnum> eventCategoryList) {
			this.eventCategoryList = eventCategoryList;
			return this;
		}

		@JsonIgnore
		public Builder eventStatusList(List<EventStatusEnum> eventStatusList) {
			this.eventStatusList = eventStatusList;
			return this;
		}

		@JsonIgnore
		public Builder startDate(LocalDateTime startDate) {
			this.startDate = startDate;
			return this;
		}

		@JsonIgnore
		public Builder endDate(LocalDateTime endDate) {
			this.endDate = endDate;
			return this;
		}

		public EventListFilter build() {
			return new EventListFilter(this);
		}

	}

}
