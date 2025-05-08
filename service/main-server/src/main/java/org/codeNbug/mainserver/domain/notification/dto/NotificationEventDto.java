package org.codeNbug.mainserver.domain.notification.dto;

import lombok.Getter;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;

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

    public NotificationEventDto(Long notificationId, Long userId, NotificationEnum type, String content) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.content = content;
    }
}
