package org.codeNbug.mainserver.domain.notification.scheduler;

import org.codeNbug.mainserver.domain.notification.service.NotificationEmitterService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SSE 연결을 유지하기 위한 하트비트 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHeartbeatScheduler {

    private final NotificationEmitterService emitterService;

    /**
     * 30초마다 모든 연결에 하트비트 메시지 전송
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        int connectionCount = emitterService.getActiveConnectionCount();
        if (connectionCount > 0) {
            log.debug("하트비트 전송 중: 활성 연결 수={}", connectionCount);
            emitterService.sendHeartbeat();
        }
    }
}