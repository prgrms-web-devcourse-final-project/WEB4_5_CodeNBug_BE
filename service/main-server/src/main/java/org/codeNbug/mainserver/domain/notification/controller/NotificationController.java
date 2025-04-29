package org.codeNbug.mainserver.domain.notification.controller;

import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.notification.dto.NotificationDto;
import org.codeNbug.mainserver.domain.notification.service.NotificationService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 임시로 사용할 사용자 ID (실제로는 인증된 사용자 정보에서 가져와야 함)
    private static final Long TEMP_USER_ID = 1L;

    /**
     * 알림 목록 조회 API
     * 페이지네이션 처리된 알림 목록을 반환
     *
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬)
     * @param userId 사용자 ID (파라미터로 받는 방식, 임시)
     * @return 알림 목록 조회 결과
     */
    @GetMapping
    public RsData<Page<NotificationDto>> getNotifications(
            @PageableDefault(size = 10, sort = "sentAt") Pageable pageable,
            @RequestParam(required = false) Long userId) {

        // 임시: userId가 제공되지 않으면 기본값 사용
        Long targetUserId = userId != null ? userId : TEMP_USER_ID;

        // 서비스 호출하여 알림 목록 조회
        Page<NotificationDto> notifications = notificationService.getNotifications(targetUserId, pageable);

        // API 명세서에 맞는 응답 형식 구성
        return new RsData<>("200-SUCCESS", "알림 목록 조회 성공", notifications);
    }
}