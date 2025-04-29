package org.codeNbug.mainserver.domain.seat.entity;

import org.codeNbug.mainserver.domain.manager.entity.Event;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
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
@Setter
public class SeatLayout {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Lob
	private String layout;
	@OneToOne
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;
}
