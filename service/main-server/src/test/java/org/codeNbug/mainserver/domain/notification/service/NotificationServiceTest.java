package org.codeNbug.mainserver.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.codeNbug.mainserver.domain.notification.dto.NotificationDto;
import org.codeNbug.mainserver.domain.notification.dto.NotificationEventDto;
import org.codeNbug.mainserver.domain.notification.entity.Notification;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;
import org.codeNbug.mainserver.domain.notification.repository.NotificationRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationEmitterService emitterService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("알림 목록 조회")
    void getNotifications() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notificationList = Arrays.asList(
                createNotification(1L, userId, NotificationEnum.SYSTEM, "시스템 알림"),
                createNotification(2L, userId, NotificationEnum.EVENT, "이벤트 알림")
        );
        Page<Notification> notificationPage = new PageImpl<>(notificationList, pageable, notificationList.size());

        when(notificationRepository.findByUserIdOrderBySentAtDesc(eq(userId), eq(pageable)))
                .thenReturn(notificationPage);

        // when
        Page<NotificationDto> result = notificationService.getNotifications(userId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getType()).isEqualTo(NotificationEnum.SYSTEM);
        assertThat(result.getContent().get(1).getType()).isEqualTo(NotificationEnum.EVENT);

        verify(notificationRepository, times(1)).findByUserIdOrderBySentAtDesc(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("읽지 않은 알림 목록 조회")
    void getUnreadNotifications() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notificationList = Arrays.asList(
                createNotification(1L, userId, NotificationEnum.SYSTEM, "시스템 알림"),
                createNotification(2L, userId, NotificationEnum.EVENT, "이벤트 알림")
        );
        Page<Notification> notificationPage = new PageImpl<>(notificationList, pageable, notificationList.size());

        when(notificationRepository.findByUserIdAndIsReadFalseOrderBySentAtDesc(eq(userId), eq(pageable)))
                .thenReturn(notificationPage);

        // when
        Page<NotificationDto> result = notificationService.getUnreadNotifications(userId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        verify(notificationRepository, times(1)).findByUserIdAndIsReadFalseOrderBySentAtDesc(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("알림 상세 조회")
    void getNotificationById() {
        // given
        Long notificationId = 1L;
        Long userId = 1L;
        Notification notification = createNotification(notificationId, userId, NotificationEnum.SYSTEM, "시스템 알림");

        when(notificationRepository.findById(eq(notificationId))).thenReturn(Optional.of(notification));

        // when
        NotificationDto result = notificationService.getNotificationById(notificationId, userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(notificationId);
        assertThat(result.getType()).isEqualTo(NotificationEnum.SYSTEM);
        assertThat(result.getContent()).isEqualTo("시스템 알림");
        assertTrue(notification.isRead()); // 조회 후 읽음 상태로 변경되었는지 확인

        verify(notificationRepository, times(1)).findById(eq(notificationId));
    }

    @Test
    @DisplayName("다른 사용자의 알림 조회 시 예외 발생")
    void getNotificationByIdWithWrongUser() {
        // given
        Long notificationId = 1L;
        Long userId = 1L;
        Long wrongUserId = 2L;
        Notification notification = createNotification(notificationId, userId, NotificationEnum.SYSTEM, "시스템 알림");

        when(notificationRepository.findById(eq(notificationId))).thenReturn(Optional.of(notification));

        // when & then
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> notificationService.getNotificationById(notificationId, wrongUserId)
        );

        assertEquals("해당 알림에 접근할 권한이 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("알림 생성")
    void createNotification() {
        // given
        Long userId = 1L;
        NotificationEnum type = NotificationEnum.SYSTEM;
        String content = "새 알림";

        when(userRepository.existsById(eq(userId))).thenReturn(true);

        Notification savedNotification = createNotification(1L, userId, type, content);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // when
        NotificationDto result = notificationService.createNotification(userId, type, content);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(type);
        assertThat(result.getContent()).isEqualTo(content);
        assertFalse(result.isRead());

        // 이벤트 발행 확인
        ArgumentCaptor<NotificationEventDto> eventCaptor = ArgumentCaptor.forClass(NotificationEventDto.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        NotificationEventDto capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.getUserId());
        assertEquals(type, capturedEvent.getType());
        assertEquals(content, capturedEvent.getContent());

        verify(userRepository, times(1)).existsById(eq(userId));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자에게 알림 생성시 예외 발생")
    void createNotificationWithNonExistingUser() {
        // given
        Long userId = 999L;
        NotificationEnum type = NotificationEnum.SYSTEM;
        String content = "새 알림";

        when(userRepository.existsById(eq(userId))).thenReturn(false);

        // when & then
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> notificationService.createNotification(userId, type, content)
        );

        assertEquals("해당 사용자를 찾을 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("알림 삭제")
    void deleteNotification() {
        // given
        Long notificationId = 1L;
        Long userId = 1L;
        Notification notification = createNotification(notificationId, userId, NotificationEnum.SYSTEM, "시스템 알림");

        when(notificationRepository.findById(eq(notificationId))).thenReturn(Optional.of(notification));
        doNothing().when(notificationRepository).delete(any(Notification.class));

        // when
        notificationService.deleteNotification(notificationId, userId);

        // then
        verify(notificationRepository, times(1)).findById(eq(notificationId));
        verify(notificationRepository, times(1)).delete(eq(notification));
    }

    @Test
    @DisplayName("다른 사용자의 알림 삭제 시 예외 발생")
    void deleteNotificationWithWrongUser() {
        // given
        Long notificationId = 1L;
        Long userId = 1L;
        Long wrongUserId = 2L;
        Notification notification = createNotification(notificationId, userId, NotificationEnum.SYSTEM, "시스템 알림");

        when(notificationRepository.findById(eq(notificationId))).thenReturn(Optional.of(notification));

        // when & then
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> notificationService.deleteNotification(notificationId, wrongUserId)
        );

        assertEquals("해당 알림에 접근할 권한이 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("다건 알림 삭제")
    void deleteNotifications() {
        // given
        Long userId = 1L;
        List<Long> notificationIds = Arrays.asList(1L, 2L);
        List<Notification> notifications = Arrays.asList(
                createNotification(1L, userId, NotificationEnum.SYSTEM, "시스템 알림"),
                createNotification(2L, userId, NotificationEnum.EVENT, "이벤트 알림")
        );

        when(notificationRepository.findAllByUserIdAndIdIn(eq(userId), eq(notificationIds)))
                .thenReturn(notifications);
        doNothing().when(notificationRepository).deleteAll(eq(notifications));

        // when
        notificationService.deleteNotifications(notificationIds, userId);

        // then
        verify(notificationRepository, times(1)).findAllByUserIdAndIdIn(eq(userId), eq(notificationIds));
        verify(notificationRepository, times(1)).deleteAll(eq(notifications));
    }

    @Test
    @DisplayName("모든 알림 삭제")
    void deleteAllNotifications() {
        // given
        Long userId = 1L;
        List<Notification> notifications = Arrays.asList(
                createNotification(1L, userId, NotificationEnum.SYSTEM, "시스템 알림"),
                createNotification(2L, userId, NotificationEnum.EVENT, "이벤트 알림")
        );

        when(notificationRepository.findByUserIdOrderBySentAtDesc(eq(userId)))
                .thenReturn(notifications);
        doNothing().when(notificationRepository).deleteAll(eq(notifications));

        // when
        notificationService.deleteAllNotifications(userId);

        // then
        verify(notificationRepository, times(1)).findByUserIdOrderBySentAtDesc(eq(userId));
        verify(notificationRepository, times(1)).deleteAll(eq(notifications));
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회")
    void getUnreadCount() {
        // given
        Long userId = 1L;
        long unreadCount = 5;

        when(notificationRepository.countByUserIdAndIsReadFalse(eq(userId)))
                .thenReturn(unreadCount);

        // when
        long result = notificationService.getUnreadCount(userId);

        // then
        assertEquals(unreadCount, result);
        verify(notificationRepository, times(1)).countByUserIdAndIsReadFalse(eq(userId));
    }

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