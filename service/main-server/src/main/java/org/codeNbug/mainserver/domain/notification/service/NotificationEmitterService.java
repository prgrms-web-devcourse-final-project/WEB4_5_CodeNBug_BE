package org.codeNbug.mainserver.domain.notification.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codeNbug.mainserver.domain.notification.dto.NotificationDto;
import org.codeNbug.mainserver.domain.notification.entity.Notification;
import org.codeNbug.mainserver.domain.notification.entity.NotificationSseConnection;
import org.codeNbug.mainserver.domain.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * SSE Emitter를 관리하는 서비스
 */
@Slf4j
@Service
public class NotificationEmitterService {
    // static 키워드 제거하여 인스턴스 변수로 변경
    private final Map<Long, NotificationSseConnection> emitterMap = new ConcurrentHashMap<>();

    // SSE 연결 타임아웃 시간 (1시간)
    private static final long SSE_CONNECTION_TIMEOUT = 60 * 60 * 1000L;

    // 알림 저장소 주입
    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationEmitterService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * 사용자의 SSE 연결을 생성하고 저장
     *
     * @param userId 사용자 ID
     * @param lastEventId 클라이언트가 마지막으로 수신한 이벤트 ID
     * @return 생성된 SSE Emitter
     */
    public SseEmitter createEmitter(Long userId, String lastEventId) {
        // 기존 연결이 있으면 제거
        removeEmitter(userId);

        // 새 연결 생성 (타임아웃 설정)
        SseEmitter emitter = new SseEmitter(SSE_CONNECTION_TIMEOUT);

        // 연결 이벤트 핸들러 등록
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: userId={}", userId);
            removeEmitter(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: userId={}", userId);
            removeEmitter(userId);
        });

        emitter.onError((e) -> {
            log.error("SSE 연결 에러: userId={}, error={}", userId, e.getMessage());
            removeEmitter(userId);
        });

        // 맵에 저장
        NotificationSseConnection connection = new NotificationSseConnection(userId, emitter, lastEventId);
        emitterMap.put(userId, connection);

        // 초기 연결 메시지 전송
        try {
            // 연결 성공 이벤트를 ID와 함께 전송
            String eventId = "connect-" + System.currentTimeMillis();
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name("connect")
                    .data("알림 구독이 시작되었습니다."));

            // 마지막 이벤트 ID가 있으면 그 이후의 알림을 전송
            if (lastEventId != null && !lastEventId.isEmpty()) {
                sendLostEvents(userId, lastEventId);
            }
        } catch (IOException e) {
            log.error("SSE 초기 메시지 전송 실패: userId={}", userId);
            removeEmitter(userId);
        }

        return emitter;
    }

    /**
     * 사용자 연결 제거
     *
     * @param userId 사용자 ID
     */
    public void removeEmitter(Long userId) {
        NotificationSseConnection connection = emitterMap.remove(userId);
        if (connection != null) {
            try {
                connection.getEmitter().complete();
            } catch (Exception e) {
                log.warn("Emitter 종료 중 예외 발생: userId={}, error={}", userId, e.getMessage());
            }
        }
    }

    /**
     * 특정 사용자에게 알림 전송
     *
     * @param userId 사용자 ID
     * @param notification 알림 정보
     */
    public void sendNotification(Long userId, NotificationDto notification) {
        NotificationSseConnection connection = emitterMap.get(userId);
        if (connection != null) {
            try {
                // 이벤트 ID 생성 (알림 ID 사용)
                String eventId = "notification-" + notification.getId();

                // 이벤트 ID를 포함하여 전송
                connection.getEmitter().send(
                        SseEmitter.event()
                                .id(eventId)
                                .name("notification")
                                .data(notification)
                );

                // 마지막 이벤트 ID 업데이트
                connection.setLastEventId(eventId);
            } catch (IOException e) {
                log.error("알림 전송 실패: userId={}, notificationId={}", userId, notification.getId());
                removeEmitter(userId);
            }
        }
    }

    /**
     * 마지막으로 받은 이벤트 ID 이후의 알림을 전송
     *
     * @param userId 사용자 ID
     * @param lastEventId 마지막으로 받은 이벤트 ID
     */
    private void sendLostEvents(Long userId, String lastEventId) {
        // Last-Event-ID에서 알림 ID 추출
        Long lastNotificationId = extractNotificationId(lastEventId);
        if (lastNotificationId == null) {
            return;
        }

        // 마지막 이벤트 ID 이후의 알림 조회 (최근 20개로 제한)
        notificationRepository.findByUserIdAndIdGreaterThanOrderByIdAsc(
                        userId, lastNotificationId, PageRequest.of(0, 20)
                )
                .forEach(notification -> {
                    try {
                        String eventId = "notification-" + notification.getId();
                        NotificationSseConnection connection = emitterMap.get(userId);
                        if (connection != null) {
                            connection.getEmitter().send(
                                    SseEmitter.event()
                                            .id(eventId)
                                            .name("notification")
                                            .data(NotificationDto.from(notification))
                            );
                            connection.setLastEventId(eventId);
                        }
                    } catch (IOException e) {
                        log.error("누락된 알림 전송 실패: userId={}, notificationId={}", userId, notification.getId());
                    }
                });
    }

    /**
     * 이벤트 ID에서 알림 ID 추출
     *
     * @param eventId 이벤트 ID (format: notification-{id})
     * @return 알림 ID 또는 null
     */
    private Long extractNotificationId(String eventId) {
        if (eventId != null && eventId.startsWith("notification-")) {
            try {
                return Long.parseLong(eventId.substring("notification-".length()));
            } catch (NumberFormatException e) {
                log.warn("잘못된 이벤트 ID 형식: {}", eventId);
            }
        }
        return null;
    }

    /**
     * 모든 연결에 하트비트 메시지 전송
     */
    public void sendHeartbeat() {
        emitterMap.forEach((userId, connection) -> {
            try {
                String heartbeatId = "heartbeat-" + System.currentTimeMillis();
                connection.getEmitter().send(
                        SseEmitter.event()
                                .id(heartbeatId)
                                .name("heartbeat")
                                .data(".")
                );
            } catch (IOException e) {
                log.debug("하트비트 전송 실패: userId={}", userId);
                removeEmitter(userId);
            }
        });
    }

    /**
     * 연결된 사용자 수 반환
     *
     * @return 현재 연결된 사용자 수
     */
    public int getActiveConnectionCount() {
        return emitterMap.size();
    }

    /**
     * 특정 사용자의 연결 여부 확인
     *
     * @param userId 사용자 ID
     * @return 연결 여부
     */
    public boolean isConnected(Long userId) {
        return emitterMap.containsKey(userId);
    }

    /**
     * 서비스 종료 시 모든 연결 정리
     */
    @PreDestroy
    public void destroy() {
        log.info("NotificationEmitterService 종료 - 모든 SSE 연결 정리");
        emitterMap.forEach((userId, connection) -> {
            try {
                connection.getEmitter().complete();
            } catch (Exception e) {
                log.warn("Emitter 종료 중 예외 발생: userId={}", userId);
            }
        });
        emitterMap.clear();
    }
}