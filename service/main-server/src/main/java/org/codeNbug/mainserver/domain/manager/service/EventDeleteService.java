package org.codeNbug.mainserver.domain.manager.service;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventDeleteService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ManagerEventRepository managerEventRepository;

    /**
     * 이벤트를 삭제 처리하는 메서드입니다.
     * 삭제 요청한 사용자가 해당 이벤트의 매니저인지 검증 후, 삭제 플래그를 true로 설정합니다.
     *
     * @param eventId 삭제할 이벤트 ID
     * @param managerId 삭제 요청을 한 매니저의 사용자 ID
     */
    @Transactional
    public void deleteEvent(Long eventId, Long managerId) {
        Event event = getEventOrThrow(eventId);
        validateManagerAuthority(managerId, event);
        event.setIsDeleted(true);
    }

    /**
     * 이벤트를 ID로 조회하는 메서드입니다.
     * 존재하지 않는 경우 예외를 발생시킵니다.
     */
    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("존재하지 않는 이벤트입니다."));
    }

    /**
     * 해당 이벤트에 대해 매니저 권한이 있는지 확인하는 메서드입니다.
     * 권한이 없을 경우 예외를 발생시킵니다.
     */
    private void validateManagerAuthority(Long managerId, Event event) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BadRequestException("존재하지 않는 매니저입니다."));
        boolean hasPermission = managerEventRepository.existsByManagerAndEvent(manager, event);
        if (!hasPermission) {
            throw new BadRequestException("이벤트에 대한 삭제 권한이 없습니다.");
        }
    }
}
