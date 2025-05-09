package org.codeNbug.mainserver.domain.seat.entity;

import java.util.ArrayList;
import java.util.List;

import org.codeNbug.mainserver.domain.event.entity.Event;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SeatLayout 엔티티 클래스
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class SeatLayout {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Setter
	@Lob
	private String layout;

	@OneToMany(mappedBy = "layout")
	private final List<Seat> seats = new ArrayList<>();
	@OneToOne
	@Setter
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;
}
