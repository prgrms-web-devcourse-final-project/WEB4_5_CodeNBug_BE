package org.codeNbug.mainserver.domain.manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterResponse;
import org.codeNbug.mainserver.domain.manager.dto.layout.LayoutDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.PriceDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.SeatInfoDto;
import org.codeNbug.mainserver.domain.manager.entity.*;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.EventTypeRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.codeNbug.mainserver.domain.user.repository.UserRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EventRegisterService {

    private final EventDomainService eventDomainService;
    private final EventRepository eventRepository;
    private final SeatLayoutRepository seatLayoutRepository;
    private final ManagerEventRepository managerEventRepository;
    private final UserRepository userRepository;


    /**
     * 이벤트 등록 메인 메서드
     */
    @Transactional
    public EventRegisterResponse registerEvent(EventRegisterRequest request, Long managerId) {
        EventType eventType = eventDomainService.findOrCreateEventType(request.getType());
        Event event = createAndSaveEvent(request, eventType);
        SeatLayout seatLayout = createAndSaveSeatLayout(request, event);
        Map<String, SeatGrade> seatGradeMap = eventDomainService.createAndSaveSeatGrades(event, request.getPrice());
        eventDomainService.createAndSaveSeats(event, seatLayout, request.getLayout(), seatGradeMap);
        saveManagerEvent(managerId, event);
        return eventDomainService.buildEventRegisterResponse(request, event);
    }

    /**
     * 이벤트 등록 시 해당 이벤트와 등록한 매니저를 ManagerEvent 에 저장하는 메서드
     * @param managerId
     * @param event
     */
    private void saveManagerEvent(Long managerId, Event event) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BadRequestException("존재하지 않는 매니저입니다."));

        managerEventRepository.save(new ManagerEvent(null, manager, event));
    }

    /**
     * Event 생성 및 저장
     */
    private Event createAndSaveEvent(EventRegisterRequest request, EventType eventType) {
        Event event = new Event(
                null,
                eventType.getEventTypeId(),
                EventInformation.builder()
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
                        .build(),
                request.getBookingStart(),
                request.getBookingEnd(),
                0, // viewCount 기본값
                null, // createdAt
                null, // modifiedAt
                EventStatusEnum.OPEN,
                true,
                false
        );

        return eventRepository.save(event);
    }

    /**
     * SeatLayout 생성 및 저장
     * - 요청된 Layout 정보를 JSON으로 직렬화 후 저장
     * - 저장된 SeatLayout은 해당 Event와 연결
     *
     * @param request 이벤트 등록 요청
     * @param event 저장된 Event
     * @return 저장된 SeatLayout
     */
    private SeatLayout createAndSaveSeatLayout(EventRegisterRequest request, Event event) {
        String layoutJson = eventDomainService.serializeLayoutToJson(request.getLayout());
        SeatLayout seatLayout = new SeatLayout(
                null,
                layoutJson,
                event
        );
        return seatLayoutRepository.save(seatLayout);
    }
}
