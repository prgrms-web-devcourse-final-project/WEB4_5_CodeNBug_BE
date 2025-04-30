package org.codeNbug.mainserver.domain.manager.service;

import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.entity.EventInformation;
import org.codeNbug.mainserver.domain.manager.entity.EventType;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.EventTypeRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventEditService {

    private final EventRepository eventRepository;
    private final SeatLayoutRepository seatLayoutRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final SeatRepository seatRepository;
    private final EventDomainService eventDomainService;
    private final EventTypeRepository eventTypeRepository;
    private final ManagerEventRepository managerEventRepository;

    /**
     * 이벤트 수정 메인 메서드
     */
    @Transactional
    public EventRegisterResponse editEvent(Long eventId, EventRegisterRequest request, Long managerId) {
        // 1. 기존 이벤트 조회
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("이벤트를 찾을 수 없습니다: id=" + eventId));

        // 2. EventType 수정
        // 기존 타입명 조회
        String currentTypeName = eventTypeRepository.findById(event.getTypeId()).orElseThrow(
                () -> new BadRequestException("이벤트 타입을 찾을 수 없습니다.")
        ).getName();
        String newTypeName = request.getType();

        // 타입명이 다를 때만 타입 변경
        if (!currentTypeName.equals(newTypeName)) {
            EventType eventType = eventDomainService.findOrCreateEventType(newTypeName);
            event.setTypeId(eventType.getEventTypeId());
        }


        // 3. EventInformation 수정
        EventInformation newInformation = EventInformation.builder()
                .title(request.getTitle())
                .thumbnailUrl(request.getThumbnailUrl())
                .description(request.getDescription())
                .ageLimit(request.getAgelimit())
                .restrictions(request.getRestriction())
                .location(request.getLocation())
                .hallName(request.getHallName())
                .eventStart(request.getStartDate())
                .eventEnd(request.getEndDate())
                .seatCount(request.getSeatCount())
                .build();
        event.setInformation(newInformation);

        // 4. 예약 기간 수정
        event.setBookingStart(request.getBookingStart());
        event.setBookingEnd(request.getBookingEnd());

        // 5. SeatLayout 수정
        SeatLayout seatLayout = seatLayoutRepository.findByEvent(event)
                .orElseThrow(() -> new BadRequestException("좌석 레이아웃을 찾을 수 없습니다: eventId=" + eventId));

        String layoutJson = eventDomainService.serializeLayoutToJson(request.getLayout());
        seatLayout.setLayout(layoutJson);

        // 6. Seat 수정
        // 기존 Seat 삭제
        List<Seat> oldSeats = seatRepository.findByEvent(event);
        seatRepository.deleteAll(oldSeats);

        // 7. SeatGrade 수정
        // 기존 SeatGrade 삭제
        List<SeatGrade> oldGrades = seatGradeRepository.findByEvent(event);
        seatGradeRepository.deleteAll(oldGrades);

        // 새로운 SeatGrade 저장
        Map<String, SeatGrade> seatGradeMap = eventDomainService.createAndSaveSeatGrades(event, request.getPrice());
        // 새로운 Seat 저장
        eventDomainService.createAndSaveSeats(event, seatLayout, request.getLayout(), seatGradeMap);

        // 8. 응답 반환
        return eventDomainService.buildEventRegisterResponse(request, event);
    }
}

