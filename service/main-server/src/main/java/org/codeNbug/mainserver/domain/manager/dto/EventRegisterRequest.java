package org.codeNbug.mainserver.domain.manager.dto;

import lombok.*;
import org.codeNbug.mainserver.domain.manager.dto.layout.LayoutDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.PriceDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegisterRequest {
    private String title;
    private String type; // 예: "MUSICAL", "CONCERT" 등
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

}
