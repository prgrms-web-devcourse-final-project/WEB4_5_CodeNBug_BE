package org.codeNbug.mainserver.domain.manager.dto;

import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;

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
    private EventCategoryEnum category;
    private String thumbnailUrl;
    private EventStatusEnum status;

    // 행사 기간
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // 예매 기간
    private LocalDateTime bookingStart;
    private LocalDateTime bookingEnd;

    private String location;
    private String hallName;
    private Boolean isDeleted;
}
