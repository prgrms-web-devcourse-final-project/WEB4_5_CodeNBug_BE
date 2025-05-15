package org.codeNbug.mainserver.domain.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.codeNbug.mainserver.domain.notification.dto.NotificationCreateRequestDto;
import org.codeNbug.mainserver.domain.notification.dto.NotificationDeleteRequestDto;
import org.codeNbug.mainserver.domain.notification.dto.NotificationDto;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;
import org.codeNbug.mainserver.domain.notification.service.NotificationEmitterService;
import org.codeNbug.mainserver.domain.notification.service.NotificationService;
import org.codeNbug.mainserver.global.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationEmitterService emitterService;

    @InjectMocks
    private NotificationController notificationController;

    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        // MockMvc 설정
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setViewResolvers((viewName, locale) -> new MappingJackson2JsonView())
                .build();

        // SecurityUtil 모킹 시작
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilMock != null) {
            securityUtilMock.close();
        }
    }

    @Test
    @WithMockUser
    @DisplayName("알림 목록 조회 테스트")
    void getNotifications() throws Exception {
        // given
        List<NotificationDto> notificationList = Arrays.asList(
                createNotificationDto(1L, NotificationEnum.SYSTEM, "시스템 알림입니다."),
                createNotificationDto(2L, NotificationEnum.EVENT, "이벤트 알림입니다."),
                createNotificationDto(3L, NotificationEnum.TICKET, "티켓 알림입니다.")
        );

        Page<NotificationDto> notificationPage = new PageImpl<>(
                notificationList,
                PageRequest.of(0, 10),
                notificationList.size()
        );

        when(notificationService.getNotifications(anyLong(), any(Pageable.class)))
                .thenReturn(notificationPage);

        // when & then
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-SUCCESS"))
                .andExpect(jsonPath("$.msg").value("알림 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3));

        verify(notificationService, times(1)).getNotifications(anyLong(), any(Pageable.class));
    }

    @Test
    @WithMockUser
    @DisplayName("읽지 않은 알림 조회 테스트")
    void getUnreadNotifications() throws Exception {
        // given
        List<NotificationDto> unreadNotifications = Arrays.asList(
                createNotificationDto(1L, NotificationEnum.SYSTEM, "읽지 않은 시스템 알림"),
                createNotificationDto(2L, NotificationEnum.EVENT, "읽지 않은 이벤트 알림")
        );

        Page<NotificationDto> notificationPage = new PageImpl<>(
                unreadNotifications,
                PageRequest.of(0, 10),
                unreadNotifications.size()
        );

        when(notificationService.getUnreadNotifications(anyLong(), any(Pageable.class)))
                .thenReturn(notificationPage);

        // when & then
        mockMvc.perform(get("/api/v1/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-SUCCESS"))
                .andExpect(jsonPath("$.msg").value("미읽은 알림 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));

        verify(notificationService, times(1)).getUnreadNotifications(anyLong(), any(Pageable.class));
    }

    @Test
    @WithMockUser
    @DisplayName("알림 상세 조회 테스트")
    void getNotificationDetail() throws Exception {
        // given
        Long notificationId = 1L;
        NotificationDto readNotification = NotificationDto.builder()
                .id(notificationId)
                .type(NotificationEnum.SYSTEM)
                .content("시스템 알림 상세")
                .sentAt(LocalDateTime.now())
                .isRead(true)
                .build();

        when(notificationService.getNotificationById(eq(notificationId), anyLong()))
                .thenReturn(readNotification);

        // when & then
        mockMvc.perform(get("/api/v1/notifications/{id}", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-SUCCESS"))
                .andExpect(jsonPath("$.msg").value("알림 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(notificationId))
                .andExpect(jsonPath("$.data.read").value(true));

        verify(notificationService, times(1)).getNotificationById(eq(notificationId), anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("알림 생성 테스트")
    void createNotification() throws Exception {
        // given
        NotificationCreateRequestDto requestDto = new NotificationCreateRequestDto(
                1L, NotificationEnum.SYSTEM, "API를 통해 생성된 테스트 알림입니다."
        );

        NotificationDto createdNotification = createNotificationDto(
                1L, NotificationEnum.SYSTEM, "API를 통해 생성된 테스트 알림입니다."
        );

        when(notificationService.createNotification(
                eq(requestDto.getUserId()),
                eq(requestDto.getType()),
                eq(requestDto.getContent())))
                .thenReturn(createdNotification);

        // when & then
        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-SUCCESS"))
                .andExpect(jsonPath("$.msg").value("알림 생성 성공"))
                .andExpect(jsonPath("$.data.type").value("SYSTEM"))
                .andExpect(jsonPath("$.data.content").value("API를 통해 생성된 테스트 알림입니다."))
                .andExpect(jsonPath("$.data.read").value(false));

        verify(notificationService, times(1)).createNotification(
                eq(requestDto.getUserId()),
                eq(requestDto.getType()),
                eq(requestDto.getContent())
        );
    }

    @Test
    @WithMockUser
    @DisplayName("단일 알림 삭제 테스트")
    void deleteNotification() throws Exception {
        // given
        Long notificationId = 1L;

        doNothing().when(notificationService).deleteNotification(eq(notificationId), anyLong());

        // when & then
        mockMvc.perform(delete("/api/v1/notifications/{id}", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-SUCCESS"))
                .andExpect(jsonPath("$.msg").value("알림 삭제 성공"));

        verify(notificationService, times(1)).deleteNotification(eq(notificationId), anyLong());
    }

    @Test
    @WithMockUser
    @DisplayName("다건 알림 삭제 테스트")
    void deleteNotifications() throws Exception {
        // given
        List<Long> notificationIds = Arrays.asList(1L, 2L);
        NotificationDeleteRequestDto request = new NotificationDeleteRequestDto(notificationIds);

        doNothing().when(notificationService).deleteNotifications(eq(notificationIds), anyLong());

        // when & then
        mockMvc.perform(delete("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-SUCCESS"))
                .andExpect(jsonPath("$.msg").value("알림 삭제 성공"));

        verify(notificationService, times(1)).deleteNotifications(eq(notificationIds), anyLong());
    }

    @Test
    @WithMockUser
    @DisplayName("모든 알림 삭제 테스트")
    void deleteAllNotifications() throws Exception {
        // given
        doNothing().when(notificationService).deleteAllNotifications(anyLong());

        // when & then
        mockMvc.perform(delete("/api/v1/notifications/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-SUCCESS"))
                .andExpect(jsonPath("$.msg").value("모든 알림 삭제 성공"));

        verify(notificationService, times(1)).deleteAllNotifications(anyLong());
    }

    @Test
    @WithMockUser
    @DisplayName("알림 구독 SSE 엔드포인트 테스트")
    void subscribeToSSE() throws Exception {
        // given
        SseEmitter mockEmitter = mock(SseEmitter.class);
        when(emitterService.createEmitter(anyLong(), anyString())).thenReturn(mockEmitter);

        // when & then
        mockMvc.perform(get("/api/v1/notifications/subscribe")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE)
                        .header("Last-Event-ID", "notification-1"))
                .andExpect(status().isOk());

        // createEmitter 메서드 호출 검증
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> lastEventIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(emitterService, times(1)).createEmitter(userIdCaptor.capture(), lastEventIdCaptor.capture());

        // Last-Event-ID 헤더 값이 올바르게 전달되었는지 검증
        String capturedLastEventId = lastEventIdCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("notification-1", capturedLastEventId);
    }

    @Test
    @WithMockUser
    @DisplayName("알림 구독 SSE - Last-Event-ID 없이 요청 시 테스트")
    void subscribeToSSEWithoutLastEventId() throws Exception {
        // given
        SseEmitter mockEmitter = mock(SseEmitter.class);
        when(emitterService.createEmitter(anyLong(), eq(null))).thenReturn(mockEmitter);

        // when & then
        mockMvc.perform(get("/api/v1/notifications/subscribe")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());

        // createEmitter 메서드 호출 검증 - Last-Event-ID 없이 호출되었는지 확인
        verify(emitterService, times(1)).createEmitter(anyLong(), eq(null));
    }


    /**
     * Builder 패턴을 사용하여 NotificationDto 객체 생성
     */
    private NotificationDto createNotificationDto(Long id, NotificationEnum type, String content) {
        return NotificationDto.builder()
                .id(id)
                .type(type)
                .content(content)
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();
    }
}