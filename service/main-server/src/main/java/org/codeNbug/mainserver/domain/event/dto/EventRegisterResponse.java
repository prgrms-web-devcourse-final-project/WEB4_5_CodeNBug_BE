package org.codeNbug.mainserver.domain.event.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.codeNbug.mainserver.domain.event.dto.layout.PriceDto;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.dto.layout.LayoutDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventRegisterResponse {
    private Long eventId;
    private String title;
    private String type;
    private String description;
    private String restriction;
    private String thumbnailUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;
    private String hallName;
    private int seatCount;
    private LayoutDto layout;
    private List<PriceDto> price;
    private LocalDateTime bookingStart;
    private LocalDateTime bookingEnd;
    private int agelimit;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private EventStatusEnum status;

}
