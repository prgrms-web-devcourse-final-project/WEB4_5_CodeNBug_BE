package org.codeNbug.mainserver.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.notification.dto.NotificationDto;
import org.codeNbug.mainserver.domain.notification.entity.Notification;
import org.codeNbug.mainserver.domain.notification.repository.NotificationRepository;
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
}