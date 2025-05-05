package org.codeNbug.mainserver.domain.event.entity;

import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.manager.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.Getter;

@Entity
@Table(name = "event")
@Getter
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long eventId;

	@Column(nullable = false)
	@Length(min = 5)
	private String title;
	@Column(nullable = false)
	private String thumbnailUrl;
	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;
	@Column(nullable = false)
	@Min(0)
	private int ageLimit;
	@Column(nullable = false, columnDefinition = "TEXT")
	private String restriction;
	@Column(nullable = false)
	@Min(0)
	private int seatCount;
	@Column(nullable = false)
	private LocalDateTime bookingStart;
	@Column(nullable = false)
	private LocalDateTime bookingEnd;
	@Column(nullable = false)
	private LocalDateTime eventStart;
	@Column(nullable = false)
	private LocalDateTime eventEnd;
	@Embedded
	private Location location;
	@Column(nullable = false)
	private String hallName;
	@Column(nullable = false)
	private Long viewCount = 0L;
	@Column(nullable = false)
	private EventStatusEnum eventStatus = EventStatusEnum.OPEN;
	@Column(nullable = false)
	private boolean seatSelectable = true;

	@OneToOne(mappedBy = "event")
	private SeatLayout seatLayout;

	@CreatedDate
	private LocalDateTime createdAt;
	@LastModifiedDate
	private LocalDateTime modifiedAt;

	public Event() {
	}

	public Event(String title, String thumbnailUrl, String description, int ageLimit, String restriction, int seatCount,
		LocalDateTime bookingStart, LocalDateTime bookingEnd, LocalDateTime eventStart, LocalDateTime eventEnd,
		Location location, String hallName) {
		if (bookingStart.isAfter(bookingEnd)) {
			throw new IllegalArgumentException("bookingStart is after bookingEnd");
		}
		if (eventStart.isAfter(eventEnd)) {
			throw new IllegalArgumentException("eventStart is after eventEnd");
		}

		this.title = title;
		this.thumbnailUrl = thumbnailUrl;
		this.description = description;
		this.ageLimit = ageLimit;
		this.restriction = restriction;
		this.seatCount = seatCount;
		this.bookingStart = bookingStart;
		this.bookingEnd = bookingEnd;
		this.eventStart = eventStart;
		this.eventEnd = eventEnd;
		this.location = location;
		this.hallName = hallName;
	}

}
