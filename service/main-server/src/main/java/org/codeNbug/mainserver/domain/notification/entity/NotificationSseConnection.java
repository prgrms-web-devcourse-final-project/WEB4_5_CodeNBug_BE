package org.codeNbug.mainserver.domain.notification.entity;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.Getter;
import lombok.Setter;

/**
 * 사용자의 SSE 연결 정보를 관리하는 클래스
 */
@Getter
public class NotificationSseConnection {
    private final SseEmitter emitter;
    private final Long userId;

    @Setter
    private boolean connected;

    private final long createdAt;

    // 마지막으로 수신한 이벤트 ID를 저장
    @Setter
    private String lastEventId;

    public NotificationSseConnection(Long userId, SseEmitter emitter, String lastEventId) {
        this.userId = userId;
        this.emitter = emitter;
        this.connected = true;
        this.createdAt = System.currentTimeMillis();
        this.lastEventId = lastEventId;
    }

    /**
     * 연결 시간(밀리초)
     */
    public long getConnectionDuration() {
        return System.currentTimeMillis() - createdAt;
    }
}