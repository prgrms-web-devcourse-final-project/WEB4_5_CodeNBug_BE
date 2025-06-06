package org.codeNbug.mainserver.domain.event.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codenbug.common.util.Util;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * DTO for {@link org.codeNbug.mainserver.domain.event.entity.Event}
 */
@Getter
@NoArgsConstructor
public class EventInfoResponse implements Serializable {

	SeatLayoutDto seatLayout;
	List<SeatLayoutDto.SeatDto.SeatGradeDto> prices;
	Long eventId;
	EventCategoryEnum category;
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
		this.category = event.getCategory();
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
			Util.fromJson(event.getSeatLayout().getLayout(), new TypeReference<Map<String, Object>>() {
			}),
			event.getSeatLayout().getSeats().stream()
				.map(seat -> new SeatLayoutDto.SeatDto(
					seat.getId(),
					seat.getLocation(),
					seat.isAvailable(),
					seat.getGrade().getGrade().name()
				)).toList()
		);
	}

	public EventInfoResponse(Event event, Integer viewCount) {
		this.eventId = event.getEventId();
		this.category = event.getCategory();
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
		this.viewCount = viewCount;
		this.createdAt = event.getCreatedAt();
		this.modifiedAt = event.getModifiedAt();
		this.status = event.getStatus();
		this.seatSelectable = event.getSeatSelectable();
		this.isDeleted = event.getIsDeleted();
		this.seatLayout = new SeatLayoutDto(
			event.getSeatLayout().getId(),
			Util.fromJson(event.getSeatLayout().getLayout(), new TypeReference<Map<String, Object>>() {
			}),
			event.getSeatLayout().getSeats().stream()
				.map(seat -> new SeatLayoutDto.SeatDto(
					seat.getId(),
					seat.getLocation(),
					seat.isAvailable(),
					seat.getGrade().getGrade().name())
				).toList());
		this.prices = event.getSeatLayout().getSeats().stream()
			.map(seat -> new SeatLayoutDto.SeatDto.SeatGradeDto(
				seat.getGrade().getId(),
				seat.getGrade().getGrade(),
				seat.getGrade().getAmount()
			)).collect(Collectors.toSet()).stream().toList();
	}

	/**
	 * DTO for {@link org.codeNbug.mainserver.domain.seat.entity.SeatLayout}
	 */
	@Value
	public static class SeatLayoutDto implements Serializable {
		Long id;
		Map<String, Object> layout;
		List<SeatDto> seats;

		/**
		 * DTO for {@link org.codeNbug.mainserver.domain.seat.entity.Seat}
		 */
		@NoArgsConstructor
		@AllArgsConstructor
		@Getter
		public static class SeatDto implements Serializable {
			Long id;
			@NotNull
			String location;
			boolean available;
			String grade;
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