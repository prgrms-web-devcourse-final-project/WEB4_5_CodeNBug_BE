package org.codeNbug.mainserver.domain.manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.manager.dto.layout.LayoutDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.PriceDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.SeatInfoDto;
import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.entity.EventType;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.EventTypeRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private final EventRepository eventRepository;
    private final EventTypeRepository eventTypeRepository;
    private final SeatLayoutRepository seatLayoutRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final SeatRepository seatRepository;
    private final ObjectMapper objectMapper;

    /**
     * 이벤트 등록 메인 메서드
     */
    @Transactional
    public EventRegisterResponse registerEvent(EventRegisterRequest request) {
        EventType eventType = findOrCreateEventType(request.getType());
        Event event = createAndSaveEvent(request, eventType);
        SeatLayout seatLayout = createAndSaveSeatLayout(request, event);
        Map<String, SeatGrade> seatGradeMap = createAndSaveSeatGrades(request, event);
        createAndSaveSeats(request, event, seatLayout, seatGradeMap);
        return buildEventRegisterResponse(request, event);
    }

    /**
     * 타입명으로 EventType을 조회하고 없으면 생성
     */
    private EventType findOrCreateEventType(String typeName) {
        return eventTypeRepository.findByName(typeName)
                .orElseGet(() -> eventTypeRepository.save(new EventType(null, typeName)));
    }

    /**
     * Event 생성 및 저장
     */
    private Event createAndSaveEvent(EventRegisterRequest request, EventType eventType) {
        Event event = new Event(
                null,
                eventType.getEventTypeId(),
                request.getTitle(),
                request.getThumbnailUrl(),
                request.getDescription(),
                request.getAgelimit(),
                request.getRestriction(),
                request.getSeatCount(),
                request.getBookingStart(),
                request.getBookingEnd(),
                request.getStartDate(),
                request.getEndDate(),
                0,
                request.getLocation(),
                request.getHallName(),
                null,
                null,
                EventStatusEnum.OPEN,
                true
        );
        return eventRepository.save(event);
    }

    /**
     * SeatLayout 생성 및 저장
     */
    private SeatLayout createAndSaveSeatLayout(EventRegisterRequest request, Event event) {
        try {
            String layoutJson = objectMapper.writeValueAsString(request.getLayout());
            SeatLayout seatLayout = new SeatLayout(
                    null,
                    layoutJson,
                    event
            );
            return seatLayoutRepository.save(seatLayout);
        } catch (Exception e) {
            throw new RuntimeException("Seat layout 직렬화 실패", e);
        }
    }

    /**
     * SeatGrade 생성 및 저장
     */
    private Map<String, SeatGrade> createAndSaveSeatGrades(EventRegisterRequest request, Event event) {
        Map<String, SeatGrade> seatGradeMap = new HashMap<>();
        for (PriceDto price : request.getPrice()) {
            SeatGrade seatGrade = new SeatGrade(
                    null,
                    SeatGradeEnum.valueOf(price.getGrade()),
                    price.getAmount(),
                    event
            );
            SeatGrade savedGrade = seatGradeRepository.save(seatGrade);
            seatGradeMap.put(price.getGrade(), savedGrade);
        }
        return seatGradeMap;
    }

    /**
     * Seat 개별 생성 및 저장
     */
    private void createAndSaveSeats(EventRegisterRequest request, Event event, SeatLayout seatLayout, Map<String, SeatGrade> seatGradeMap) {
        LayoutDto layoutDto = request.getLayout();
        for (List<String> row : layoutDto.getLayout()) {
            for (String seatName : row) {
                if (seatName == null) continue;

                SeatInfoDto seatInfo = layoutDto.getSeat().get(seatName);
                if (seatInfo == null) {
                    throw new IllegalStateException("SeatInfo not found for seat: " + seatName);
                }

                SeatGrade seatGrade = seatGradeMap.get(seatInfo.getGrade());
                if (seatGrade == null) {
                    throw new IllegalStateException("SeatGrade not found for grade: " + seatInfo.getGrade());
                }

                Seat seat = new Seat(
                        null,
                        seatName,
                        true,
                        seatGrade,
                        seatLayout,
                        null,
                        event
                );
                seatRepository.save(seat);
            }
        }
    }

    /**
     * 최종 응답 객체 생성
     */
    private EventRegisterResponse buildEventRegisterResponse(EventRegisterRequest request, Event event) {
        return EventRegisterResponse.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .type(request.getType())
                .description(request.getDescription())
                .restriction(request.getRestriction())
                .thumbnailUrl(event.getThumbnailUrl())
                .startDate(event.getEventStart())
                .endDate(event.getEventEnd())
                .location(event.getLocation())
                .hallName(event.getHallName())
                .seatCount(event.getSeatCount())
                .layout(request.getLayout())
                .price(request.getPrice())
                .bookingStart(event.getBookingStart())
                .bookingEnd(event.getBookingEnd())
                .agelimit(event.getAgeLimit())
                .createdAt(event.getCreatedAt())
                .modifiedAt(event.getModifiedAt())
                .status(event.getStatus())
                .build();
    }
}
