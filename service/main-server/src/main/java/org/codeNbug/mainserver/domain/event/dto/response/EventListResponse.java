package org.codeNbug.mainserver.domain.event.dto.response;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;

import lombok.Value;

/**
 * DTO for {@link org.codeNbug.mainserver.domain.event.entity.Event}
 */
@Value
public class EventListResponse implements Serializable {
	Long eventId;
	Long typeId;
	EventInformationDto information;
	LocalDateTime bookingStart;
	LocalDateTime bookingEnd;
	Integer viewCount;
	EventStatusEnum status;
	Boolean seatSelectable;
	Boolean isDeleted;

	public EventListResponse(Event event) {
		this.eventId = event.getEventId();
		this.typeId = event.getTypeId();
		this.information = new EventInformationDto(event.getInformation());
		this.bookingStart = event.getBookingStart();
		this.bookingEnd = event.getBookingEnd();
		this.viewCount = event.getViewCount();
		this.status = event.getStatus();
		this.seatSelectable = event.getSeatSelectable();
		this.isDeleted = event.getIsDeleted();
	}

	/**
	 * DTO for {@link org.codeNbug.mainserver.domain.event.entity.EventInformation}
	 */
	@Value
	public static class EventInformationDto implements Serializable {
		String title;
		String thumbnailUrl;
		Integer ageLimit;
		String hallName;
		LocalDateTime eventStart;
		LocalDateTime eventEnd;

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