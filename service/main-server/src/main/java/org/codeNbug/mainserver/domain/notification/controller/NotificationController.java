package org.codeNbug.mainserver.domain.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.notification.dto.NotificationCreateRequestDto;
import org.codeNbug.mainserver.domain.notification.dto.NotificationDto;
import org.codeNbug.mainserver.domain.notification.service.NotificationService;
import org.codeNbug.mainserver.domain.user.constant.UserRole;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codeNbug.mainserver.global.security.annotation.RoleRequired;
import org.codeNbug.mainserver.global.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * 알림 관련 API 엔드포인트를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회 API
     * 페이지네이션 처리된 알림 목록을 반환
     *
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬)
     * @return 알림 목록 조회 결과
     */
    @GetMapping
    public RsData<Page<NotificationDto>> getNotifications(
            @PageableDefault(size = 10, sort = "sentAt") Pageable pageable) {

        // 현재 인증된 사용자 ID 가져오기
        Long userId = SecurityUtil.getCurrentUserId();

        // 서비스 호출하여 알림 목록 조회
        Page<NotificationDto> notifications = notificationService.getNotifications(userId, pageable);

        // API 명세서에 맞는 응답 형식 구성
        return new RsData<>("200-SUCCESS", "알림 목록 조회 성공", notifications);
    }

    /**
     * 알림 상세 조회 API
     * 특정 알림의 상세 내용을 조회하고, 읽음 상태를 업데이트합니다.
     *
     * @param id 조회할 알림 ID
     * @return 알림 상세 조회 결과
     */
    @GetMapping("/{id}")
    public ResponseEntity<RsData<NotificationDto>> getNotificationDetail(@PathVariable Long id) {
        {
            // 현재 인증된 사용자 ID 가져오기
            Long userId = SecurityUtil.getCurrentUserId();

            // 서비스 호출하여 알림 상세 조회 및 읽음 처리
            NotificationDto notification = notificationService.getNotificationById(id, userId);

            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "알림 조회 성공", notification)
            );
        }
    }
    /**
     * 알림 생성 API
     * (관리자/시스템 전용) 특정 사용자에게 새로운 알림을 생성합니다
     *
     * @param requestDto 알림 생성 요청 정보
     * @return 생성된 알림 정보
     */
    @PostMapping
    @RoleRequired({UserRole.ADMIN, UserRole.MANAGER}) // 관리자, 매니저만 접근 가능
    public ResponseEntity<RsData<NotificationDto>> createNotification(
            @RequestBody @Valid NotificationCreateRequestDto requestDto) {

        // 서비스 호출하여 알림 생성
        NotificationDto createdNotification = notificationService.createNotification(
                requestDto.getUserId(),
                requestDto.getType(),
                requestDto.getContent()
        );

        return ResponseEntity.ok(
                new RsData<>("200-SUCCESS", "알림 생성 성공", createdNotification)
        );
    }

    /**
     * 미읽은 알림 조회 API
     * 페이지네이션 처리된 미읽은 알림 목록을 반환
     *
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬)
     * @return 미읽은 알림 목록 조회 결과
     */
    @GetMapping("/unread")
    public RsData<Page<NotificationDto>> getUnreadNotifications(
            @PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long userId = SecurityUtil.getCurrentUserId();
        Page<NotificationDto> unreadPage = notificationService.getUnreadNotifications(userId, pageable);

        return new RsData<>("200-SUCCESS", "미읽은 알림 조회 성공", unreadPage);
    }
}