package org.codeNbug.mainserver.domain.event.dto.response;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO for {@link org.codeNbug.mainserver.domain.event.entity.Event}
 */
@NoArgsConstructor
@Getter
public class EventListResponse implements Serializable {
	Long eventId;
	EventCategoryEnum category;
	EventInformationDto information;
	LocalDateTime bookingStart;
	LocalDateTime bookingEnd;
	Integer viewCount;
	EventStatusEnum status;
	Boolean seatSelectable;
	Boolean isDeleted;
	Integer minPrice;
	Integer maxPrice;

	public EventListResponse(Event event) {
		this.eventId = event.getEventId();
		this.category = event.getCategory();
		this.information = new EventInformationDto(event.getInformation());
		this.bookingStart = event.getBookingStart();
		this.bookingEnd = event.getBookingEnd();
		this.viewCount = event.getViewCount();
		this.status = event.getStatus();
		this.seatSelectable = event.getSeatSelectable();
		this.isDeleted = event.getIsDeleted();
	}

	public EventListResponse(Event event, Integer minPrice, Integer maxPrice) {
		this.eventId = event.getEventId();
		this.category = event.getCategory();
		this.information = new EventInformationDto(event.getInformation());
		this.bookingStart = event.getBookingStart();
		this.bookingEnd = event.getBookingEnd();
		this.viewCount = event.getViewCount();
		this.status = event.getStatus();
		this.seatSelectable = event.getSeatSelectable();
		this.isDeleted = event.getIsDeleted();
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
	}


	/**
	 * DTO for {@link org.codeNbug.mainserver.domain.event.entity.EventInformation}
	 */
	@NoArgsConstructor
	@Getter
	public static class EventInformationDto implements Serializable {
		private String title;
		private String thumbnailUrl;
		private Integer ageLimit;
		private String hallName;
		private LocalDateTime eventStart;
		private LocalDateTime eventEnd;

		public EventInformationDto(EventInformation information) {
			this.title = information.getTitle();
			this.thumbnailUrl = information.getThumbnailUrl();
			this.ageLimit = information.getAgeLimit();
			this.hallName = information.getHallName();
			this.eventStart = information.getEventStart();
			this.eventEnd = information.getEventEnd();
		}
	}
}