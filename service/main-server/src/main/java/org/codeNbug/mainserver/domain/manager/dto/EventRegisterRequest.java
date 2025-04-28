package org.codeNbug.mainserver.domain.manager.dto;

import lombok.*;

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
    private Layout layout;
    private List<Price> price;
    private LocalDateTime bookingStart;
    private LocalDateTime bookingEnd;
    private int agelimit;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Layout {
        private List<List<String>> layout; // 좌석 배치 (2차원 배열 구조)
        private Map<String, SeatInfo> seat; // 좌석 상세 정보
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatInfo {
        private String grade; // 좌석 등급 (예: A, S 등)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Price {
        private String grade; // 좌석 등급
        private int amount;   // 가격
    }
}
