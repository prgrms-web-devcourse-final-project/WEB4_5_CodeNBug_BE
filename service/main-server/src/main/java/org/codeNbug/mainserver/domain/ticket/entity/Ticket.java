package org.codeNbug.mainserver.domain.ticket.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Ticket 엔티티 클래스
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Ticket {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String seatInfo;

	@CreatedDate
	private LocalDateTime createdAt;

	@ManyToOne
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@ManyToOne
	@JoinColumn(name = "purchase_id", nullable = false)
	private Purchase purchase;
}
