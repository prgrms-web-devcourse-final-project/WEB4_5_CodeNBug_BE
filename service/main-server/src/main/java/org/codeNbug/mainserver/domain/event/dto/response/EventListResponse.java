package org.codeNbug.mainserver.domain.event.dto.response;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.event.entity.Event;
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
	private Long eventId;
	private Long typeId;
	private EventInformationDto information;
	private LocalDateTime bookingStart;
	private LocalDateTime bookingEnd;
	private Integer viewCount;
	private EventStatusEnum status;
	private Boolean seatSelectable;
	private Boolean isDeleted;

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