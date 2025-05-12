package org.codeNbug.mainserver.domain.event.entity;

import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@FilterDef(name = "activeEventFilter", parameters = @ParamDef(name = "isDeleted", type = Boolean.class))
@Filter(name = "activeEventFilter", condition = "is_deleted = :isDeleted")
public class Event {

	@Setter
	@OneToOne(mappedBy = "event")
	private SeatLayout seatLayout;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long eventId;

//	@Column(nullable = false)
//	@Setter
//	private Long typeId;

	@Setter
	@Enumerated(EnumType.STRING)
	private EventCategoryEnum category;

	@Embedded
	@Setter
	private EventInformation information;

	@Setter
	private LocalDateTime bookingStart;

	@Setter
	private LocalDateTime bookingEnd;
	@Setter
	@Column(columnDefinition = "int default 0")
	private Integer viewCount;

	@CreatedDate
	private LocalDateTime createdAt;

	@LastModifiedDate
	private LocalDateTime modifiedAt;
	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private EventStatusEnum status;
	@Setter
	@Column(columnDefinition = "boolean default true")
	private Boolean seatSelectable;
	@Setter
	@Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
	private Boolean isDeleted = false;


	public Event(EventCategoryEnum category, EventInformation information, LocalDateTime bookingStart, LocalDateTime bookingEnd,
		Integer viewCount, LocalDateTime createdAt, LocalDateTime modifiedAt, EventStatusEnum status,
		Boolean seatSelectable, Boolean isDeleted, SeatLayout seatLayout) {
		this.category = category;
		this.information = information;
		this.bookingStart = bookingStart;
		this.bookingEnd = bookingEnd;
		this.viewCount = viewCount;
		this.createdAt = createdAt;
		this.modifiedAt = modifiedAt;
		this.status = status;
		this.seatSelectable = seatSelectable;
		this.isDeleted = isDeleted;
		this.seatLayout = seatLayout;
	}
}
