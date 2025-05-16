package org.codeNbug.mainserver.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeNbug.mainserver.domain.notification.entity.NotificationStatus;
import org.codeNbug.mainserver.domain.notification.dto.NotificationDto;
import org.codeNbug.mainserver.domain.notification.dto.NotificationEventDto;
import org.codeNbug.mainserver.domain.notification.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 이벤트 리스너
 * 트랜잭션 완료 후 알림 전송 처리를 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventService {
    private final NotificationEmitterService emitterService;
    private final NotificationRepository notificationRepository;

    /**
     * 알림 생성 이벤트를 처리
     * 트랜잭션 완료(커밋) 후에 실행됨
     *
     * @param event 알림 생성 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreatedEvent(NotificationEventDto event) {
        Long userId = event.getUserId();
        Long notificationId = event.getNotificationId();

        log.debug("알림 이벤트 처리 시작: notificationId={}, userId={}", notificationId, userId);

        try{

            // 사용자가 연결되어 있는 경우에만 알림 전송
            if (emitterService.isConnected(userId)) {
                log.debug("사용자 연결 확인됨, 알림 전송 시작: userId={}", userId);

                // 이벤트에서 직접 DTO 생성 (DB 조회 제거)
                NotificationDto dto = event.toNotificationDto();
                emitterService.sendNotification(userId, dto);

                // 상태 업데이트만 DB 호출 (벌크 업데이트 사용)
                updateNotificationStatus(notificationId, NotificationStatus.SENT);
                log.debug("알림 전송 완료: notificationId={}", notificationId);
            } else {
                log.debug("사용자가 연결되어 있지 않음, 알림 미전송: userId={}", userId);
                // PENDING 상태 유지 (별도 업데이트 필요 없음)
            }
        } catch (Exception e) {
            log.error("알림 전송 중 오류 발생: notificationId={}, error={}", notificationId, e.getMessage(), e);
            updateNotificationStatus(notificationId, NotificationStatus.FAILED);
        }
    }

    /**
     * 알림 상태 업데이트
     *
     * @param notificationId 알림 ID
     * @param status 업데이트할 상태
     */
    @Transactional
    public void updateNotificationStatus(Long notificationId, NotificationStatus status) {
        try {
            // 직접 UPDATE 쿼리 실행 (엔티티 조회 없음)
            int updatedCount = notificationRepository.updateStatus(notificationId, status);

            if (updatedCount > 0) {
                log.debug("알림 상태 업데이트: notificationId={}, status={}", notificationId, status);
            } else {
                log.warn("알림 상태 업데이트 실패: 대상 알림이 없음. notificationId={}", notificationId);
            }
        } catch (Exception e) {
            log.error("알림 상태 업데이트 실패: notificationId={}, status={}, error={}",
                    notificationId, status, e.getMessage(), e);
        }
    }
}