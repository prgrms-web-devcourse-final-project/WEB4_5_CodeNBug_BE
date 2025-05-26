package org.codeNbug.mainserver.domain.manager.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import org.codeNbug.mainserver.domain.event.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.manager.dto.ManagerEventListResponse;
import org.codeNbug.mainserver.domain.manager.dto.layout.LayoutDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.PriceDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.SeatInfoDto;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codenbug.common.util.Util;
import org.codenbug.user.domain.user.entity.User;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManagerEventSearchService {

    private final ManagerEventRepository managerEventRepository;
    private final EventRepository eventRepository;
    private final EventDomainService eventDomainService;
    private final SeatGradeRepository seatGradeRepository;

    /**
     * API 요청 사용자 (매니저) 가 담당하는 이벤트 목록을 ManagerEventListResponse Dto List 형식에 맞춰 return 하는 메서드입니다.
     * @param currentUser
     * @return
     */
    public List<ManagerEventListResponse> searchEventList(User currentUser) {
        List<Event> eventsByManager = managerEventRepository.findEventsByManager(currentUser);
        return eventsByManager.stream()
                .map(event -> ManagerEventListResponse.builder()
                        .eventId(event.getEventId())
                        .title(event.getInformation().getTitle())
                        .category(event.getCategory())
                        .thumbnailUrl(event.getInformation().getThumbnailUrl())
                        .status(event.getStatus())
                        .startDate(event.getInformation().getEventStart())
                        .endDate(event.getInformation().getEventEnd())
                        .bookingStart(event.getBookingStart())
                        .bookingEnd(event.getBookingEnd())
                        .location(event.getInformation().getLocation())
                        .hallName(event.getInformation().getHallName())
                        .isDeleted(event.getIsDeleted())
                        .build())
                .toList();

    }

    @Transactional(readOnly = true)
    public EventRegisterResponse searchEvent(Long eventId, Long currentUserId) {
        // 1. fetch join으로 조회 (seats까지 한 번에 초기화)
        Event event = eventRepository.findByIdWithSeats(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));

        // 2. 매니저 권한 검증
        eventDomainService.validateManagerAuthority(currentUserId, event);

        // 3. EventType 조회
        EventCategoryEnum category = event.getCategory();

        // 4. EventInformation
        EventInformation info = event.getInformation();

        // 5. SeatLayout → LayoutDto
        LayoutDto layoutDto = null;
        SeatLayout seatLayout = event.getSeatLayout();
        if (seatLayout != null) {
            // seatLayout.layout = {"layout": [...], "seat": {...}} 형태 → layout만 추출
            Map<String, Object> fullLayoutMap = Util.fromJson(
                    seatLayout.getLayout(),
                    new TypeReference<Map<String, Object>>() {}
            );

            Object layoutObj = fullLayoutMap.get("layout");
            List<List<String>> layoutRows = Util.convertValue(
                    layoutObj,
                    new TypeReference<List<List<String>>>() {}
            );

            Map<String, SeatInfoDto> seatMap = seatLayout.getSeats().stream()
                    .collect(Collectors.toMap(
                            Seat::getLocation,
                            seat -> SeatInfoDto.builder()
                                    .grade(seat.getGrade().getGrade().name())
                                    .build()
                    ));

            layoutDto = LayoutDto.builder()
                    .layout(layoutRows)
                    .seat(seatMap)
                    .build();
        }

        // 6. 가격 정보 → SeatGrade
        List<PriceDto> priceDtos = seatGradeRepository.findByEvent(event).stream()
                .map(seatGrade -> PriceDto.builder()
                        .grade(seatGrade.getGrade().name())
                        .amount(seatGrade.getAmount())
                        .build())
                .collect(Collectors.toList());

        // 7. 최종 응답
        return EventRegisterResponse.builder()
                .eventId(event.getEventId())
                .title(info.getTitle())
                .category(category)
                .description(info.getDescription())
                .restriction(info.getRestrictions())
                .thumbnailUrl(info.getThumbnailUrl())
                .startDate(info.getEventStart())
                .endDate(info.getEventEnd())
                .location(info.getLocation())
                .hallName(info.getHallName())
                .seatCount(info.getSeatCount() != null ? info.getSeatCount() : 0)
                .layout(layoutDto)
                .price(priceDtos)
                .bookingStart(event.getBookingStart())
                .bookingEnd(event.getBookingEnd())
                .agelimit(info.getAgeLimit() != null ? info.getAgeLimit() : 0)
                .createdAt(event.getCreatedAt())
                .modifiedAt(event.getModifiedAt())
                .status(event.getStatus())
                .build();
    }
}
