package org.codeNbug.mainserver.domain.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EventType {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long eventTypeId;

	@Column(nullable = false, unique = true)
	private String name;
}
