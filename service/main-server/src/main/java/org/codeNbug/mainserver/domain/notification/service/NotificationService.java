package org.codeNbug.mainserver.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.notification.dto.NotificationDto;
import org.codeNbug.mainserver.domain.notification.entity.Notification;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;
import org.codeNbug.mainserver.domain.notification.repository.NotificationRepository;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationEmitterService emitterService;

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
     *
     * @param userId 알림을 받을 사용자 ID
     * @param type 알림 유형
     * @param content 알림 내용
     * @return 생성된 알림 DTO
     * @throws BadRequestException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public NotificationDto createNotification(Long userId, NotificationEnum type, String content) {
        // 사용자 존재 여부 검증
        if (!userRepository.existsById(userId)) {
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

        // DTO로 변환
        NotificationDto notificationDto = NotificationDto.from(savedNotification);

        // SSE로 실시간 알림 전송 (연결된 사용자에게만)
        if (emitterService.isConnected(userId)) {
            emitterService.sendNotification(userId, notificationDto);
        }

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
}