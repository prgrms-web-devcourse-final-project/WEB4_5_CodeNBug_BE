package org.codeNbug.mainserver.domain.manager.service;

import java.util.List;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.manager.repository.ManagerEventRepository;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;
import org.codeNbug.mainserver.domain.notification.service.NotificationService;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventDeleteService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ManagerEventRepository managerEventRepository;
    private final NotificationService notificationService;
    private final PurchaseRepository purchaseRepository;

    /**
     * 이벤트를 삭제 처리하는 메서드입니다.
     * 삭제 요청한 사용자가 해당 이벤트의 매니저인지 검증 후, 삭제 플래그를 true로 설정합니다.
     *
     * @param eventId 삭제할 이벤트 ID
     * @param managerId 삭제 요청을 한 매니저의 사용자 ID
     */
    @Transactional
    public void deleteEvent(Long eventId, Long managerId) throws IllegalAccessException {
        Event event = getEventOrThrow(eventId);
        validateManagerAuthority(managerId, event);

        // 이벤트기 이미 삭제되었다면 400에러 전송
        if (event.getIsDeleted()) {
            throw new IllegalAccessException("이미 삭제된 이벤트입니다.");
        }

        // 이벤트 상태 변경
        event.setIsDeleted(true);
        event.setStatus(EventStatusEnum.CANCELLED);

        // 알림 처리는 메인 로직과 분리하여 예외 처리
        try {
            // 해당 이벤트 구매자들 조회
            List<Purchase> purchases = purchaseRepository.findAllByEventId(eventId);

            // 모든 구매자에게 행사 취소 알림 전송
            String notificationContent = String.format(
                    "[%s] 행사가 취소되었습니다. 예매 내역을 확인해주세요.",
                    event.getInformation().getTitle()
            );

            for (Purchase purchase : purchases) {
                try {
                    Long userId = purchase.getUser().getUserId();
                    notificationService.createNotification(
                            userId,
                            NotificationEnum.EVENT,
                            notificationContent
                    );
                } catch (Exception e) {
                    log.error("행사 취소 알림 전송 실패. 사용자ID: {}, 구매ID: {}, 오류: {}",
                            purchase.getUser().getUserId(), purchase.getId(), e.getMessage(), e);
                    // 개별 사용자 알림 실패는 다른 사용자 알림이나 이벤트 취소에 영향을 주지 않음
                }
            }
        } catch (Exception e) {
            log.error("행사 취소 알림 처리 실패. 이벤트ID: {}, 오류: {}", eventId, e.getMessage(), e);
            // 알림 전체 실패는 이벤트 취소에 영향을 주지 않도록 예외를 무시함
        }
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
