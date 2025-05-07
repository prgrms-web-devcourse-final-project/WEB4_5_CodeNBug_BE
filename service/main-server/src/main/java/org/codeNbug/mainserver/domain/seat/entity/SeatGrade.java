package org.codeNbug.mainserver.domain.seat.entity;

import java.util.ArrayList;
import java.util.List;

import org.codeNbug.mainserver.domain.event.entity.Event;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SeatGrade 엔티티 클래스
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SeatGrade {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private SeatGradeEnum grade;

	private Integer amount;

	@ManyToOne
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@OneToMany(mappedBy = "grade")
	private final List<Seat> seats = new ArrayList<>();
}
