package org.codeNbug.mainserver.domain.manager.service;

import java.util.List;

import org.codeNbug.mainserver.domain.manager.dto.ManagerEventListResponse;
import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.entity.EventType;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.EventTypeRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codenbug.user.entity.User;
import org.codenbug.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManagerEventSearchService {

    private final ManagerEventRepository managerEventRepository;
    private final EventTypeRepository eventTypeRepository;


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
                        .eventType(getEventTypeName(event.getTypeId()))
                        .thumbnailUrl(event.getInformation().getThumbnailUrl())
                        .status(event.getStatus())
                        .startDate(event.getInformation().getEventStart())
                        .endDate(event.getInformation().getEventEnd())
                        .location(event.getInformation().getLocation())
                        .hallName(event.getInformation().getHallName())
                        .isDeleted(event.getIsDeleted())
                        .build())
                .toList();

    }

    /**
     * Event 의 type id 를 통해 실제 Event Type Name 을 찾는 보조 메서드입니다.
     * @param typeId
     * @return
     */
    private String getEventTypeName(Long typeId) {
        return eventTypeRepository.findById(typeId)
                .map(EventType::getName)
                .orElse("알 수 없음");
    }

}
