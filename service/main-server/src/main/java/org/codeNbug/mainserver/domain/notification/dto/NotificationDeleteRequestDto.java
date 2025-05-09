package org.codeNbug.mainserver.domain.notification.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 다건 알림 삭제 요청을 위한 DTO 클래스
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeleteRequestDto {
    private List<Long> notificationIds;
}