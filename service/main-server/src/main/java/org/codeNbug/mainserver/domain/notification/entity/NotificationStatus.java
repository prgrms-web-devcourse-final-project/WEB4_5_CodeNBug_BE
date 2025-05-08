package org.codeNbug.mainserver.domain.notification.entity;

/**
 * 알림 상태를 나타내는 열거형
 */
public enum NotificationStatus {
    PENDING,    // 생성됐지만 아직 전송되지 않음
    SENT,       // 성공적으로 전송됨
    FAILED      // 전송 실패
}