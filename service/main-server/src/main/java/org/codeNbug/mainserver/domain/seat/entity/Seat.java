package org.codeNbug.mainserver.domain.seat.entity;

import org.codeNbug.mainserver.domain.mock.entity.Ticket;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
public class Seat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	private String location;

	@NotNull
	private boolean available = true;

	@OneToOne
	@JoinColumn(name = "grade_id", nullable = false)
	private SeatGrade gradeId;

	@ManyToOne
	@JoinColumn(name = "layout_id", nullable = false)
	private SeatLayout layout;

	@OneToOne
	@JoinColumn(name = "ticket_id")
	private Ticket ticketId;

	@ManyToOne
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	public void reserve() {
		if (!available) {
			throw new IllegalStateException("좌석이 이미 예약되었습니다.");
		}
		this.available = false;
	}

	public void cancelReservation() {
		this.available = true;
	}
}
