package org.codeNbug.mainserver.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeNbug.mainserver.domain.notification.entity.NotificationStatus;
import org.codeNbug.mainserver.domain.notification.dto.NotificationDto;
import org.codeNbug.mainserver.domain.notification.entity.Notification;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;
import org.codeNbug.mainserver.domain.notification.dto.NotificationEventDto;
import org.codeNbug.mainserver.domain.notification.repository.NotificationRepository;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationEmitterService emitterService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 특정 사용자의 알림 목록을 페이지네이션하여 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬)
     * @return 페이징된 알림 DTO 목록
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserIdOrderBySentAtDesc(userId, pageable);
        return notifications.map(NotificationDto::from);
    }

    /**
     * 특정 알림의 상세 정보를 조회하고 읽음 상태로 업데이트
     *
     * @param notificationId 조회할 알림 ID
     * @param userId 현재 인증된 사용자 ID
     * @return 알림 상세 정보 DTO
     * @throws BadRequestException 알림을 찾을 수 없거나 권한이 없는 경우
     */
    @Transactional
    public NotificationDto getNotificationById(Long notificationId, Long userId) {
        // 알림 조회
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("해당 알림을 찾을 수 없습니다."));

        // 권한 확인 (본인의 알림인지)
        if (!notification.getUserId().equals(userId)) {
            throw new BadRequestException("해당 알림에 접근할 권한이 없습니다.");
        }

        // 읽음 상태 업데이트
        if (!notification.isRead()) {
            notification.markAsRead();
            // 트랜잭션 내에서 자동 저장됨
        }

        return NotificationDto.from(notification);
    }
    /**
     * 새로운 알림을 생성합니다
     * 트랜잭션 동기화를 위해 이벤트 발행 방식 사용
     * @param userId 알림을 받을 사용자 ID
     * @param type 알림 유형
     * @param content 알림 내용
     * @return 생성된 알림 DTO
     * @throws BadRequestException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public NotificationDto createNotification(Long userId, NotificationEnum type, String content) {
        log.debug("알림 생성 시작: userId={}, type={}", userId, type);

        // 사용자 존재 여부 검증
        if (!userRepository.existsById(userId)) {
            log.warn("알림 생성 실패: 사용자가 존재하지 않음 - userId={}", userId);
            throw new BadRequestException("해당 사용자를 찾을 수 없습니다.");
        }

        // 알림 엔티티 생성
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .content(content)
                .build();

        // 저장
        Notification savedNotification = notificationRepository.save(notification);
        log.debug("알림 저장 완료: notificationId={}", savedNotification.getId());

        // DTO로 변환
        NotificationDto notificationDto = NotificationDto.from(savedNotification);

        // 확장된 이벤트 DTO 생성 및 발행
        NotificationEventDto eventDto = NotificationEventDto.from(savedNotification);
        eventPublisher.publishEvent(eventDto);

        log.debug("알림 이벤트 발행 완료: notificationId={}", savedNotification.getId());

        return notificationDto;
    }

    /**
     * 사용자의 미읽은 알림 목록을 페이지네이션하여 조회합니다
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 미읽은 알림 DTO 목록
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUnreadNotifications(Long userId, Pageable pageable) {
        return notificationRepository
                .findByUserIdAndIsReadFalseOrderBySentAtDesc(userId, pageable)
                .map(NotificationDto::from);
    }

    /**
     * 시스템 이벤트 발생 시 알림을 생성합니다
     * 티켓 구매, 환불, 행사 시작 등의 이벤트에서 호출됩니다
     *
     * @param userId 사용자 ID
     * @param type 알림 유형
     * @param content 알림 내용
     * @return 생성된 알림 DTO
     */
    @Transactional
    public NotificationDto createSystemNotification(Long userId, NotificationEnum type, String content) {
        // 기존 createNotification 메서드 활용
        return createNotification(userId, type, content);
    }

    /**
     * 알림 상태별 카운트 조회
     *
     * @param userId 사용자 ID
     * @return 상태별 알림 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * 실패한 알림 재전송 시도
     *
     * @param notificationId 알림 ID
     * @return 재전송 성공 여부
     */
    @Transactional
    public boolean retryFailedNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElse(null);

        if (notification == null || notification.getStatus() != NotificationStatus.FAILED) {
            return false;
        }

        // 상태를 PENDING으로 변경
        notification.updateStatus(NotificationStatus.PENDING);
        notificationRepository.save(notification);

        // 이벤트 재발행
        NotificationEventDto eventDto = NotificationEventDto.from(notification);
        eventPublisher.publishEvent(eventDto);

        return true;
    }

    /**
     * 단일 알림을 삭제합니다
     *
     * @param notificationId 삭제할 알림 ID
     * @param userId 현재 인증된 사용자 ID
     * @throws BadRequestException 알림이 존재하지 않거나 권한이 없는 경우
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("해당 알림을 찾을 수 없습니다."));

        // 권한 확인 (본인의 알림인지)
        if (!notification.getUserId().equals(userId)) {
            throw new BadRequestException("해당 알림에 접근할 권한이 없습니다.");
        }

        notificationRepository.delete(notification);
        log.debug("알림 삭제 완료: notificationId={}, userId={}", notificationId, userId);
    }

    /**
     * 여러 알림을 삭제합니다(멱등성 보장)
     *
     * @param notificationIds 삭제할 알림 ID 목록
     * @param userId 현재 인증된 사용자 ID
     */
    @Transactional
    public void deleteNotifications(List<Long> notificationIds, Long userId) {
        // 사용자의 알림 중 요청된 ID 목록에 해당하는 알림만 조회
        List<Notification> notifications = notificationRepository.findAllByUserIdAndIdIn(userId, notificationIds);

        // 찾은 알림 개수와 요청 개수의 차이 로깅
        if (notifications.size() < notificationIds.size()) {
            log.info("요청된 알림 중 일부가 이미 삭제됨: 요청={}, 실제 삭제={}",
                    notificationIds.size(), notifications.size());
        }

        // 존재하는 알림만 삭제
        notificationRepository.deleteAll(notifications);
        log.debug("다건 알림 삭제 완료: count={}, userId={}", notifications.size(), userId);
    }

    /**
     * 사용자의 모든 알림을 삭제합니다
     *
     * @param userId 현재 인증된, 사용자 ID
     */
    @Transactional
    public void deleteAllNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderBySentAtDesc(userId);
        notificationRepository.deleteAll(notifications);
        log.debug("모든 알림 삭제 완료: count={}, userId={}", notifications.size(), userId);
    }
}