package org.codeNbug.mainserver.domain.manager.dto;

import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.manager.entity.EventStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ManagerEventListResponse {
	private Long eventId;
	private String title;
	private String eventType;
	private String thumbnailUrl;
	private EventStatusEnum status;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private String location;
	private String hallName;
}
