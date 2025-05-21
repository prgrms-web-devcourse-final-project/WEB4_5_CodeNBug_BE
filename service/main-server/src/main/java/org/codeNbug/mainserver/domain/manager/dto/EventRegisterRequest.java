package org.codeNbug.mainserver.domain.manager.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.manager.dto.layout.LayoutDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.PriceDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.codeNbug.mainserver.global.util.UtcToKstLocalDateTimeDeserializer;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegisterRequest {
    private String title;
    private EventCategoryEnum category;
    private String description;
    private String restriction;
    private String thumbnailUrl;
    @JsonDeserialize(using = UtcToKstLocalDateTimeDeserializer.class)
    private LocalDateTime startDate;
    @JsonDeserialize(using = UtcToKstLocalDateTimeDeserializer.class)
    private LocalDateTime endDate;
    private String location;
    private String hallName;
    private int seatCount;
    private LayoutDto layout;
    private List<PriceDto> price;
    @JsonDeserialize(using = UtcToKstLocalDateTimeDeserializer.class)
    private LocalDateTime bookingStart;
    @JsonDeserialize(using = UtcToKstLocalDateTimeDeserializer.class)
    private LocalDateTime bookingEnd;
    private int agelimit;

}
