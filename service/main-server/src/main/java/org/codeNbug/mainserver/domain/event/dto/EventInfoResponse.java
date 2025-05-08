package org.codeNbug.mainserver.domain.event.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

/**
 * DTO for {@link org.codeNbug.mainserver.domain.event.entity.Event}
 */
@Value
public class EventInfoResponse implements Serializable {
	SeatLayoutDto seatLayout;
	Long eventId;
	Long typeId;
	EventInformationDto information;
	LocalDateTime bookingStart;
	LocalDateTime bookingEnd;
	Integer viewCount;
	LocalDateTime createdAt;
	LocalDateTime modifiedAt;
	EventStatusEnum status;
	Boolean seatSelectable;
	Boolean isDeleted;

	public EventInfoResponse(Event event) {
		this.eventId = event.getEventId();
		this.typeId = event.getTypeId();
		this.information = new EventInformationDto(
			event.getInformation().getTitle(),
			event.getInformation().getThumbnailUrl(),
			event.getInformation().getDescription(),
			event.getInformation().getAgeLimit(),
			event.getInformation().getRestrictions(),
			event.getInformation().getLocation(),
			event.getInformation().getHallName(),
			event.getInformation().getEventStart(),
			event.getInformation().getEventEnd(),
			event.getInformation().getSeatCount()
		);
		this.bookingStart = event.getBookingStart();
		this.bookingEnd = event.getBookingEnd();
		this.viewCount = event.getViewCount();
		this.createdAt = event.getCreatedAt();
		this.modifiedAt = event.getModifiedAt();
		this.status = event.getStatus();
		this.seatSelectable = event.getSeatSelectable();
		this.isDeleted = event.getIsDeleted();
		this.seatLayout = new SeatLayoutDto(
			event.getSeatLayout().getId(),
			event.getSeatLayout().getLayout(),
			event.getSeatLayout().getSeats().stream()
				.map(seat -> new SeatLayoutDto.SeatDto(
					seat.getId(),
					seat.getLocation(),
					seat.isAvailable(),
					new SeatLayoutDto.SeatDto.SeatGradeDto(
						seat.getGrade().getId(),
						seat.getGrade().getGrade(),
						seat.getGrade().getAmount()
					)
				)).toList()
		);
	}

	/**
	 * DTO for {@link org.codeNbug.mainserver.domain.seat.entity.SeatLayout}
	 */
	@Value
	public static class SeatLayoutDto implements Serializable {
		Long id;
		String layout;
		List<SeatDto> seats;

		/**
		 * DTO for {@link org.codeNbug.mainserver.domain.seat.entity.Seat}
		 */
		@Value
		public static class SeatDto implements Serializable {
			Long id;
			@NotNull
			String location;
			boolean available;
			SeatGradeDto grade;

			/**
			 * DTO for {@link org.codeNbug.mainserver.domain.seat.entity.SeatGrade}
			 */
			@Value
			public static class SeatGradeDto implements Serializable {
				Long id;
				SeatGradeEnum grade;
				Integer amount;
			}
		}
	}

	/**
	 * DTO for {@link org.codeNbug.mainserver.domain.event.entity.EventInformation}
	 */
	@Value
	public static class EventInformationDto implements Serializable {
		String title;
		String thumbnailUrl;
		String description;
		Integer ageLimit;
		String restrictions;
		String location;
		String hallName;
		LocalDateTime eventStart;
		LocalDateTime eventEnd;
		Integer seatCount;
	}
}