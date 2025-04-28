package org.codeNbug.mainserver.domain.manager.dto;

import lombok.*;
import org.codeNbug.mainserver.domain.manager.entity.EventStatusEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private Layout layout;
    private List<Price> price;
    private LocalDateTime bookingStart;
    private LocalDateTime bookingEnd;
    private int agelimit;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private EventStatusEnum status;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Layout {
        private List<List<String>> layout;
        private Map<String, SeatInfo> seat;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatInfo {
        private String grade;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Price {
        private String grade;
        private int amount;
    }
}
