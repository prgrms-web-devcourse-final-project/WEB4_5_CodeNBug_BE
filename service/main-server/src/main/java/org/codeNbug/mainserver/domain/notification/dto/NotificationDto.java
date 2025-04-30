package org.codeNbug.mainserver.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.codeNbug.mainserver.domain.notification.entity.Notification;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;

import java.time.LocalDateTime;

/**
 * 알림 정보 전송용 DTO 클래스
 * 클라이언트에 전달할 알림 정보를 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private NotificationEnum type;
    private String content;
    private LocalDateTime sentAt;
    private boolean isRead;

    public static NotificationDto from(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .content(notification.getContent())
                .sentAt(notification.getSentAt())
                .isRead(notification.isRead())
                .build();
    }
}