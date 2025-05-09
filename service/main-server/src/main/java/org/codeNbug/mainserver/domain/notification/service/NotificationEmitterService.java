package org.codeNbug.mainserver.domain.notification.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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
    // 사용자별 연결 목록을 관리하는 맵
    private final Map<Long, List<NotificationSseConnection>> userConnectionsMap = new ConcurrentHashMap<>();

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

        // 새 연결 생성 (타임아웃 설정)
        SseEmitter emitter = new SseEmitter(SSE_CONNECTION_TIMEOUT);

        // 연결 객체 생성
        NotificationSseConnection connection = new NotificationSseConnection(userId, emitter, lastEventId);

        // 사용자별 연결 목록 가져오기 (없으면 새로 생성)
        List<NotificationSseConnection> connections = userConnectionsMap
                .computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

        // 연결 이벤트 핸들러 등록
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: userId={}", userId);
            removeConnection(userId, connection);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: userId={}", userId);
            removeConnection(userId, connection);
        });

        emitter.onError((e) -> {
            log.error("SSE 연결 에러: userId={}, error={}", userId, e.getMessage());
            removeConnection(userId, connection);
        });

        // 맵에 저장
        connections.add(connection);
        log.info("새 SSE 연결 추가: userId={}, 현재 연결 수={}", userId, connections.size());

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
                sendLostEvents(userId, connection);
            }
        } catch (IOException e) {
            log.error("SSE 초기 메시지 전송 실패: userId={}", userId);
            removeConnection(userId, connection);
        }

        return emitter;
    }

    /**
     * 특정 사용자 연결 제거
     *
     * @param userId 사용자 ID
     * @param connection 제거할 연결 객체
     */
    private void removeConnection(Long userId, NotificationSseConnection connection) {
        List<NotificationSseConnection> connections = userConnectionsMap.get(userId);
        if (connections != null) {
            boolean removed = connections.remove(connection);
            if (removed) {
                log.info("SSE 연결 제거: userId={}, 남은 연결 수={}", userId, connections.size());

                // 사용자의 모든 연결이 종료된 경우 맵에서 제거
                if (connections.isEmpty()) {
                    userConnectionsMap.remove(userId);
                    log.info("사용자의 모든 SSE 연결 종료: userId={}", userId);
                }
            }

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
        List<NotificationSseConnection> connections = userConnectionsMap.get(userId);
        if (connections != null && !connections.isEmpty()) {
            // 해당 사용자의 모든 연결에 알림 전송
            List<NotificationSseConnection> deadConnections = connections.stream()
                    .filter(connection -> {
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
                            return false; // 전송 성공, 제거하지 않음
                        } catch (IOException e) {
                            log.error("알림 전송 실패: userId={}, notificationId={}", userId, notification.getId());
                            return true; // 전송 실패, 제거 필요
                        }
                    })
                    .collect(Collectors.toList());

            // 실패한 연결 제거
            if (!deadConnections.isEmpty()) {
                deadConnections.forEach(connection -> removeConnection(userId, connection));
            }
        }
    }

    /**
     * 마지막으로 받은 이벤트 ID 이후의 알림을 전송
     *
     * @param userId 사용자 ID
     * @param connection 연결 객체
     */
    private void sendLostEvents(Long userId, NotificationSseConnection connection) {
        // Last-Event-ID에서 알림 ID 추출
        Long lastNotificationId = extractNotificationId(connection.getLastEventId());
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
                            connection.getEmitter().send(
                                    SseEmitter.event()
                                            .id(eventId)
                                            .name("notification")
                                            .data(NotificationDto.from(notification))
                            );
                            connection.setLastEventId(eventId);

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
        userConnectionsMap.forEach((userId, connections) -> {
            List<NotificationSseConnection> deadConnections = connections.stream()
                    .filter(connection -> {
                        try {
                            String heartbeatId = "heartbeat-" + System.currentTimeMillis();
                            connection.getEmitter().send(
                                    SseEmitter.event()
                                            .id(heartbeatId)
                                            .name("heartbeat")
                                            .data(".")
                            );
                            return false; // 전송 성공, 제거하지 않음
                        } catch (IOException e) {
                            log.debug("하트비트 전송 실패: userId={}", userId);
                            return true; // 전송 실패, 제거 필요
                        }
                    })
                    .collect(Collectors.toList());

            // 실패한 연결 제거
            if (!deadConnections.isEmpty()) {
                deadConnections.forEach(connection -> removeConnection(userId, connection));
            }
        });
    }

    /**
     * 연결된 사용자 수 반환
     *
     * @return 현재 연결된 사용자 수
     */
    public int getActiveConnectionCount() {
        return userConnectionsMap.size();
    }

    /**
     * 모든 활성 연결 수 반환
     *
     * @return 총 활성 연결 수
     */
    public int getTotalConnectionCount() {
        return userConnectionsMap.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * 특정 사용자의 연결 여부 확인
     *
     * @param userId 사용자 ID
     * @return 연결 여부
     */
    public boolean isConnected(Long userId) {
        List<NotificationSseConnection> connections = userConnectionsMap.get(userId);
        return connections != null && !connections.isEmpty();
    }

    /**
     * 특정 사용자의 연결 수 반환
     *
     * @param userId 사용자 ID
     * @return 연결 수
     */
    public int getConnectionCount(Long userId) {
        List<NotificationSseConnection> connections = userConnectionsMap.get(userId);
        return connections != null ? connections.size() : 0;
    }

    /**
     * 서비스 종료 시 모든 연결 정리
     */
    @PreDestroy
    public void destroy() {
        log.info("NotificationEmitterService 종료 - 모든 SSE 연결 정리");
        userConnectionsMap.forEach((userId, connections) -> {
            connections.forEach(connection -> {
                try {
                    connection.getEmitter().complete();
                } catch (Exception e) {
                    log.warn("Emitter 종료 중 예외 발생: userId={}", userId);
                }
            });
        });
        userConnectionsMap.clear();
    }
}