package org.codeNbug.mainserver.domain.manager.service;

import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventDeleteService {

    private final EventRepository eventRepository;

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new BadRequestException("존재하지 않는 이벤트 입니다.")
        );
        event.setIsDeleted(true);
    }
}
