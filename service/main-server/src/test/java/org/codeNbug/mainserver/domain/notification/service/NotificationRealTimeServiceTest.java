package org.codeNbug.mainserver.domain.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.codeNbug.mainserver.domain.notification.dto.NotificationDto;
import org.codeNbug.mainserver.domain.notification.dto.NotificationEventDto;
import org.codeNbug.mainserver.domain.notification.entity.Notification;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;
import org.codeNbug.mainserver.domain.notification.entity.NotificationSseConnection;
import org.codeNbug.mainserver.domain.notification.entity.NotificationStatus;
import org.codeNbug.mainserver.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

@ExtendWith(MockitoExtension.class)
class NotificationRealTimeServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    // EmitterService 테스트용 객체
    @InjectMocks
    private NotificationEmitterService emitterService;

    // EventService 테스트용 객체
    @InjectMocks
    private NotificationEventService notificationEventService;

    // 리플렉션으로 접근하기 위한 userConnectionsMap 필드명
    private static final String USER_CONNECTIONS_MAP_FIELD = "userConnectionsMap";

    @BeforeEach
    void setUp() throws Exception {
        // userConnectionsMap 초기화
        java.lang.reflect.Field field = NotificationEmitterService.class.getDeclaredField(USER_CONNECTIONS_MAP_FIELD);
        field.setAccessible(true);
        field.set(emitterService, new ConcurrentHashMap<>());

        // NotificationEventService의 emitterService 필드 설정
        java.lang.reflect.Field emitterServiceField = NotificationEventService.class.getDeclaredField("emitterService");
        emitterServiceField.setAccessible(true);
        emitterServiceField.set(notificationEventService, emitterService);

        // NotificationEventService의 notificationRepository 필드 설정
        java.lang.reflect.Field notificationRepositoryField = NotificationEventService.class.getDeclaredField("notificationRepository");
        notificationRepositoryField.setAccessible(true);
        notificationRepositoryField.set(notificationEventService, notificationRepository);
    }

    // ---------- EmitterService 테스트 ----------

    @Test
    @DisplayName("SSE 이미터 생성 테스트")
    void createEmitter() throws Exception {
        // given
        Long userId = 1L;
        String lastEventId = null;

        // when
        SseEmitter result = emitterService.createEmitter(userId, lastEventId);

        // then
        assertNotNull(result);

        // userConnectionsMap에 연결이 추가되었는지 확인
        java.lang.reflect.Field field = NotificationEmitterService.class.getDeclaredField(USER_CONNECTIONS_MAP_FIELD);
        field.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) field.get(emitterService);
        assertTrue(map.containsKey(userId));

        // 연결 정보 확인
        assertEquals(true, emitterService.isConnected(userId));
        assertEquals(1, emitterService.getConnectionCount(userId));
    }

    @Test
    @DisplayName("알림 전송 테스트")
    void sendNotification() throws Exception {
        // given
        Long userId = 1L;

        // 먼저 이미터 생성
        SseEmitter mockEmitter = mock(SseEmitter.class);

        // userConnectionsMap에 연결 추가 (테스트 설정)
        java.lang.reflect.Field field = NotificationEmitterService.class.getDeclaredField(USER_CONNECTIONS_MAP_FIELD);
        field.setAccessible(true);
        Map<Long, List<NotificationSseConnection>> map = (Map<Long, List<NotificationSseConnection>>) field.get(emitterService);

        // NotificationSseConnection 생성 및 userConnectionsMap에 추가
        NotificationSseConnection connection = new NotificationSseConnection(userId, mockEmitter, null);
        map.put(userId, new CopyOnWriteArrayList<>(Arrays.asList(connection)));

        // 전송할 알림 생성
        NotificationDto notification = NotificationDto.builder()
                .id(1L)
                .type(NotificationEnum.SYSTEM)
                .content("테스트 알림")
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        // when
        emitterService.sendNotification(userId, notification);

        // then
        // send 메서드가 호출되었는지 확인 (SseEventBuilder 타입으로 명확히 지정)
        verify(mockEmitter, times(1)).send(any(SseEventBuilder.class));
    }

    @Test
    @DisplayName("하트비트 전송 테스트")
    void sendHeartbeat() throws Exception {
        // given
        Long userId = 1L;

        // SseEmitter 모킹
        SseEmitter mockEmitter = mock(SseEmitter.class);

        // userConnectionsMap에 연결 추가 (테스트 설정)
        java.lang.reflect.Field field = NotificationEmitterService.class.getDeclaredField(USER_CONNECTIONS_MAP_FIELD);
        field.setAccessible(true);
        Map<Long, List<NotificationSseConnection>> map = (Map<Long, List<NotificationSseConnection>>) field.get(emitterService);

        // NotificationSseConnection 생성 및 userConnectionsMap에 추가
        NotificationSseConnection connection = new NotificationSseConnection(userId, mockEmitter, null);
        map.put(userId, new CopyOnWriteArrayList<>(Arrays.asList(connection)));

        // when
        emitterService.sendHeartbeat();

        // then
        // send 메서드가 호출되었는지 확인 (SseEventBuilder 타입으로 명확히 지정)
        verify(mockEmitter, times(1)).send(any(SseEventBuilder.class));
    }

    // ---------- NotificationEventService 테스트 ----------

    @Test
    @DisplayName("알림 생성 이벤트 처리 - 사용자가 연결되어 있는 경우")
    void handleNotificationCreatedEventWithConnectedUser() throws Exception {
        // given
        Long notificationId = 1L;
        Long userId = 1L;
        NotificationEnum type = NotificationEnum.SYSTEM;
        String content = "테스트 알림";

        NotificationEventDto eventDto = new NotificationEventDto(notificationId, userId, type, content);

        // emitterService 설정 - 사용자가 연결되어 있음
        SseEmitter mockEmitter = mock(SseEmitter.class);

        // userConnectionsMap에 연결 추가 (테스트 설정)
        java.lang.reflect.Field field = NotificationEmitterService.class.getDeclaredField(USER_CONNECTIONS_MAP_FIELD);
        field.setAccessible(true);
        Map<Long, List<NotificationSseConnection>> map = (Map<Long, List<NotificationSseConnection>>) field.get(emitterService);

        // NotificationSseConnection 생성 및 userConnectionsMap에 추가
        NotificationSseConnection connection = new NotificationSseConnection(userId, mockEmitter, null);
        map.put(userId, new CopyOnWriteArrayList<>(Arrays.asList(connection)));

        // 알림 조회 결과 모킹
        Notification notification = createNotification(notificationId, userId, type, content);
        when(notificationRepository.findById(eq(notificationId))).thenReturn(Optional.of(notification));

        // 알림 상태 업데이트를 mock으로 대체
        NotificationEventService spyEventService = org.mockito.Mockito.spy(notificationEventService);
        doNothing().when(spyEventService).updateNotificationStatus(anyLong(), any(NotificationStatus.class));

        // when
        spyEventService.handleNotificationCreatedEvent(eventDto);

        // then
        verify(notificationRepository, times(1)).findById(eq(notificationId));
        verify(spyEventService, times(1)).updateNotificationStatus(eq(notificationId), eq(NotificationStatus.SENT));

        // 알림이 전송되었는지 확인 (mockEmitter.send가 호출되었는지)
        verify(mockEmitter, times(1)).send(any(SseEventBuilder.class));
    }

    @Test
    @DisplayName("알림 생성 이벤트 처리 - 사용자가 연결되어 있지 않은 경우")
    void handleNotificationCreatedEventWithDisconnectedUser() throws Exception {
        // given
        Long notificationId = 1L;
        Long userId = 1L;
        NotificationEnum type = NotificationEnum.SYSTEM;
        String content = "테스트 알림";

        NotificationEventDto eventDto = new NotificationEventDto(notificationId, userId, type, content);

        // 알림 조회 결과 모킹
        Notification notification = createNotification(notificationId, userId, type, content);
        when(notificationRepository.findById(eq(notificationId))).thenReturn(Optional.of(notification));

        // userConnectionsMap은 비어있으므로 사용자가 연결되어 있지 않음

        // 알림 상태 업데이트를 mock으로 대체
        NotificationEventService spyEventService = org.mockito.Mockito.spy(notificationEventService);
        doNothing().when(spyEventService).updateNotificationStatus(anyLong(), any(NotificationStatus.class));

        // when
        spyEventService.handleNotificationCreatedEvent(eventDto);

        // then
        verify(notificationRepository, times(1)).findById(eq(notificationId));

        // 연결되지 않았으므로 상태 업데이트 호출되지 않아야 함
        verify(spyEventService, never()).updateNotificationStatus(anyLong(), any(NotificationStatus.class));
    }

    @Test
    @DisplayName("알림 생성 이벤트 처리 - 전송 실패 시 상태 업데이트")
    void handleNotificationCreatedEventSendFailed() throws Exception {
        // given
        Long notificationId = 1L;
        Long userId = 1L;
        NotificationEnum type = NotificationEnum.SYSTEM;
        String content = "테스트 알림";

        NotificationEventDto eventDto = new NotificationEventDto(notificationId, userId, type, content);

        // emitterService 설정 - 사용자가 연결되어 있음
        SseEmitter mockEmitter = mock(SseEmitter.class);

        // send 메서드 호출 시 예외 발생하도록 설정
        doAnswer(invocation -> {
            throw new IOException("전송 실패");
        }).when(mockEmitter).send(any(SseEventBuilder.class));

        // userConnectionsMap에 연결 추가 (테스트 설정)
        java.lang.reflect.Field field = NotificationEmitterService.class.getDeclaredField(USER_CONNECTIONS_MAP_FIELD);
        field.setAccessible(true);
        Map<Long, List<NotificationSseConnection>> map = (Map<Long, List<NotificationSseConnection>>) field.get(emitterService);

        // NotificationSseConnection 생성 및 userConnectionsMap에 추가
        NotificationSseConnection connection = new NotificationSseConnection(userId, mockEmitter, null);
        map.put(userId, new CopyOnWriteArrayList<>(Arrays.asList(connection)));

        // 알림 조회 결과 모킹
        Notification notification = createNotification(notificationId, userId, type, content);
        when(notificationRepository.findById(eq(notificationId))).thenReturn(Optional.of(notification));

        // 알림 상태 업데이트를 mock으로 대체
        NotificationEventService spyEventService = org.mockito.Mockito.spy(notificationEventService);
        doNothing().when(spyEventService).updateNotificationStatus(anyLong(), any(NotificationStatus.class));

        // when
        spyEventService.handleNotificationCreatedEvent(eventDto);

        // then
        verify(notificationRepository, times(1)).findById(eq(notificationId));

        // 실패 상태로 업데이트 확인
        verify(spyEventService, times(1)).updateNotificationStatus(eq(notificationId), eq(NotificationStatus.FAILED));
    }

    @Test
    @DisplayName("알림 상태 업데이트 테스트")
    void updateNotificationStatus() throws Exception {
        // given
        Long notificationId = 1L;
        NotificationStatus status = NotificationStatus.SENT;

        Notification notification = createNotification(notificationId, 1L, NotificationEnum.SYSTEM, "테스트 알림");
        when(notificationRepository.findById(eq(notificationId))).thenReturn(Optional.of(notification));

        // when
        notificationEventService.updateNotificationStatus(notificationId, status);

        // then
        verify(notificationRepository, times(1)).findById(eq(notificationId));
        verify(notificationRepository, times(1)).save(any(Notification.class));

        // 상태가 업데이트되었는지 확인 (리플렉션으로 확인이 어려울 수 있음)
    }

    /**
     * 테스트용 Notification 객체 생성 도우미 메서드
     */
    private Notification createNotification(Long id, Long userId, NotificationEnum type, String content) {
        Notification notification = new Notification(userId, type, content);

        // 리플렉션으로 private 필드 설정
        try {
            java.lang.reflect.Field idField = Notification.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(notification, id);

            java.lang.reflect.Field sentAtField = Notification.class.getDeclaredField("sentAt");
            sentAtField.setAccessible(true);
            sentAtField.set(notification, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return notification;
    }
}