package org.codeNbug.mainserver.domain.notification.service;

import org.codeNbug.mainserver.domain.notification.dto.NotificationDto;
import org.codeNbug.mainserver.domain.notification.entity.Notification;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;
import org.codeNbug.mainserver.domain.notification.repository.NotificationRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.user.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * 알림 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

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

        // DTO로 변환하여 반환
        return NotificationDto.from(savedNotification);
    }
}