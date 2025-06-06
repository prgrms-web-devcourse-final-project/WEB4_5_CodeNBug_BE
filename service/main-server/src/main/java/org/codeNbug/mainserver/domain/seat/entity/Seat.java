package org.codeNbug.mainserver.domain.seat.entity;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Seat 엔티티 클래스
 */
@Entity
@Table(indexes = {
	/*
	외래키는 자동으로 인덱스르 생성합니다. 필요 없어서 주석 처리했습니다
	 */
	// @Index(name = "idx_seat_event_id", columnList = "event_id"),
	// @Index(name = "idx_seat_grade_id", columnList = "grade_id"),
	// @Index(name = "idx_seat_layout_id", columnList = "layout_id"),
	// @Index(name = "idx_seat_available", columnList = "available")
})
@NoArgsConstructor
@Getter
@Builder
public class Seat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	private String location;

	@Setter
	@NotNull
	private boolean available = true;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "grade_id", nullable = false)
	private SeatGrade grade;

	@ManyToOne
	@JoinColumn(name = "layout_id", nullable = false)
	private SeatLayout layout;

	@Setter
	@ManyToOne
	@JoinColumn(name = "ticket_id")
	private Ticket ticket;

	@ManyToOne
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	public Seat(Long id, String location, boolean available, SeatGrade grade, SeatLayout layout, Ticket ticket,
		Event event) {
		this.id = id;
		this.location = location;
		this.available = available;
		this.grade = grade;
		this.layout = layout;
		this.ticket = ticket;
		this.event = event;
		layout.getSeats().add(this);
	}

	public void reserve() {
		this.available = false;
	}

	public void cancelReserve() {
		this.available = true;
	}
}
