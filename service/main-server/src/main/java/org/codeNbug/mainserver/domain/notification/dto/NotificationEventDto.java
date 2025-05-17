package org.codeNbug.mainserver.domain.notification.dto;

import lombok.Getter;
import org.codeNbug.mainserver.domain.notification.entity.Notification;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;
import org.codeNbug.mainserver.domain.notification.entity.NotificationStatus;

import java.time.LocalDateTime;

/**
 * 알림 생성 이벤트 클래스
 * 트랜잭션 동기화를 위해 사용됨
 */
@Getter
public class NotificationEventDto {
    private final Long notificationId;
    private final Long userId;
    private final NotificationEnum type;
    private final String content;
    private final LocalDateTime sentAt;
    private final boolean isRead;
    private final NotificationStatus status;

    public NotificationEventDto(Long notificationId, Long userId, NotificationEnum type, String content,
                                LocalDateTime sentAt, boolean isRead, NotificationStatus status) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.sentAt = sentAt;
        this.isRead = isRead;
        this.status = status;
    }

    public static NotificationEventDto from(Notification notification) {
        return new NotificationEventDto(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getContent(),
                notification.getSentAt(),
                notification.isRead(),
                notification.getStatus()
        );
    }

    // 알림 DTO로 변환하는 메서드 추가
    public NotificationDto toNotificationDto() {
        return NotificationDto.builder()
                .id(notificationId)
                .type(type)
                .content(content)
                .sentAt(sentAt)
                .isRead(isRead)
                .build();
    }

}
