package org.codeNbug.mainserver.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeNbug.mainserver.domain.notification.entity.NotificationStatus;
import org.codeNbug.mainserver.domain.notification.dto.NotificationDto;
import org.codeNbug.mainserver.domain.notification.dto.NotificationEventDto;
import org.codeNbug.mainserver.domain.notification.entity.Notification;
import org.codeNbug.mainserver.domain.notification.repository.NotificationRepository;
import org.codeNbug.mainserver.domain.notification.service.NotificationEmitterService;
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

        log.debug("알림 이벤트 처리 시작: notificationId={}, userId={}", event.getNotificationId(), userId);

        try {
            // DB에서 알림 조회 (트랜잭션이 완료되었으므로 조회 가능)
            Notification notification = notificationRepository
                    .findById(event.getNotificationId())
                    .orElse(null);

            if (notification == null) {
                log.warn("알림을 찾을 수 없음: notificationId={}", event.getNotificationId());
                return;
            }

            // 사용자가 연결되어 있는 경우에만 알림 전송
            if (emitterService.isConnected(userId)) {
                log.debug("사용자 연결 확인됨, 알림 전송 시작: userId={}", userId);
                NotificationDto dto = NotificationDto.from(notification);
                emitterService.sendNotification(userId, dto);

                // 알림 상태 업데이트 (SENT로 변경)
                updateNotificationStatus(notification.getId(), NotificationStatus.SENT);
                log.debug("알림 전송 완료: notificationId={}", notification.getId());
            } else {
                log.debug("사용자가 연결되어 있지 않음, 알림 미전송: userId={}", userId);
                // 연결되지 않은 경우에도 PENDING 상태로 유지 (추후 연결 시 전송 가능하도록)
            }
        } catch (Exception e) {
            log.error("알림 전송 중 오류 발생: notificationId={}, error={}", event.getNotificationId(), e.getMessage(), e);
            // 실패한 경우 상태 업데이트 (FAILED로 변경)
            updateNotificationStatus(event.getNotificationId(), NotificationStatus.FAILED);
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
            Notification notification = notificationRepository.findById(notificationId)
                    .orElse(null);

            if (notification != null) {
                notification.updateStatus(status);
                notificationRepository.save(notification);
                log.debug("알림 상태 업데이트: notificationId={}, status={}", notificationId, status);
            }
        } catch (Exception e) {
            log.error("알림 상태 업데이트 실패: notificationId={}, status={}, error={}",
                    notificationId, status, e.getMessage(), e);
        }
    }
}