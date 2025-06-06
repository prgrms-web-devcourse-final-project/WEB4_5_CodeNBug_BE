package org.codeNbug.mainserver.domain.event.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventInformation {

	@Column(nullable = false)
	private String title;

	private String thumbnailUrl;

	@Lob
	private String description;

	private Integer ageLimit;

	@Lob
	private String restrictions;

	private String location;

	private String hallName;

	private LocalDateTime eventStart;

	private LocalDateTime eventEnd;

	private Integer seatCount;
}
