package org.codeNbug.mainserver.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationEnum type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = true, length = 500)
    private String content;

    @Column(nullable = true, length = 500)
    private String targetUrl;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private boolean isRead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Builder
    public Notification(Long userId, NotificationEnum type, String title, String content, String targetUrl) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.targetUrl = targetUrl;
        this.isRead = false;
        this.status = NotificationStatus.PENDING;
    }

    // 하위 호환성을 위한 생성자 (기존 코드 호출 지원)
    @Builder(builderMethodName = "legacyBuilder")
    public Notification(Long userId, NotificationEnum type, String content) {
        this.userId = userId;
        this.type = type;
        this.title = extractTitleFromContent(content);
        this.content = content;
        this.targetUrl = null;
        this.isRead = false;
        this.status = NotificationStatus.PENDING;
    }

    // 내용에서 제목 추출 (내용이 짧으면 그대로, 길면 앞부분만 사용)
    private String extractTitleFromContent(String content) {
        if (content == null || content.isEmpty()) return "알림";
        if (content.length() <= 30) return content;
        return content.substring(0, 30) + "...";
    }


    /**
     * 알림을 읽음 상태로 변경
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * 알림 상태 업데이트
     * @param status 새로운 상태
     */
    public void updateStatus(NotificationStatus status) {
        this.status = status;
    }
}