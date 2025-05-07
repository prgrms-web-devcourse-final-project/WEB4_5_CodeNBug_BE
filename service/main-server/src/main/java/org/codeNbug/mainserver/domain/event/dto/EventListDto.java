package org.codeNbug.mainserver.domain.event.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;

import lombok.Value;

/**
 * DTO for {@link org.codeNbug.mainserver.domain.event.entity.Event}
 */
@Value
public class EventListDto implements Serializable {
	Long eventId;
	Long typeId;
	EventInformationDto information;
	LocalDateTime bookingStart;
	LocalDateTime bookingEnd;
	Integer viewCount;
	EventStatusEnum status;
	Boolean seatSelectable;
	Boolean isDeleted;

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
	}
}