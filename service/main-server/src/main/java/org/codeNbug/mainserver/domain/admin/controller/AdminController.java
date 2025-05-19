package org.codeNbug.mainserver.domain.admin.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeNbug.mainserver.domain.admin.dto.request.AdminLoginRequest;
import org.codeNbug.mainserver.domain.admin.dto.request.AdminSignupRequest;
import org.codeNbug.mainserver.domain.admin.dto.request.BulkUserIdsRequest;
import org.codeNbug.mainserver.domain.admin.dto.request.RoleUpdateRequest;
import org.codeNbug.mainserver.domain.admin.dto.response.AdminLoginResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.AdminSignupResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.DashboardStatsResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.EventAdminDto;
import org.codeNbug.mainserver.domain.admin.dto.response.ModifyRoleResponse;
import org.codeNbug.mainserver.domain.admin.service.AdminService;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.security.annotation.RoleRequired;
import org.codenbug.user.security.exception.AuthenticationFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

/**
 * 관리자 컨트롤러
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    /**
     * 관리자 대시보드 페이지
     */
    @RoleRequired(UserRole.ADMIN)
    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        // 인증 정보 확인 로직 (필요 시 구현)
        log.info(">> 관리자 대시보드 페이지 요청");

        // 쿠키에서 토큰 확인
        Cookie[] cookies = request.getCookies();
        boolean hasToken = false;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    log.info(">> 액세스 토큰이 존재합니다: {}", cookie.getValue());
                    hasToken = true;
                    break;
                }
            }
        }

        if (!hasToken) {
            log.warn(">> 액세스 토큰이 없습니다. 인증 정보를 확인하세요.");
        }

        // 대시보드 통계 정보 조회
        try {
            DashboardStatsResponse stats = adminService.getDashboardStats();
            model.addAttribute("stats", stats);
            log.info(">> 대시보드 통계 정보 조회 성공: 사용자={}, 이벤트={}",
                    stats.getTotalUsers(), stats.getTotalEvents());
        } catch (Exception e) {
            log.error(">> 대시보드 통계 정보 조회 실패: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "통계 정보를 불러오는 데 실패했습니다.");
        }

        model.addAttribute("welcomeMessage", "관리자 대시보드에 오신 것을 환영합니다!");
        return "admin/dashboard";
    }

    /**
     * 관리자 로그인 페이지
     */
    @GetMapping("/login")
    public String loginPage(Model model) {
        log.info(">> 관리자 로그인 페이지 요청");
        model.addAttribute("adminLoginRequest", new AdminLoginRequest());
        return "admin/login";
    }

    /**
     * 관리자 로그인 처리
     */
    @PostMapping("/login")
    public String login(@ModelAttribute AdminLoginRequest request,
                        BindingResult bindingResult,
                        HttpServletResponse response,
                        RedirectAttributes redirectAttributes,
                        @RequestParam(required = false) String email,
                        @RequestParam(required = false) String password) {

        log.info(">> 관리자 로그인 요청: 이메일={}", email);

        // AdminLoginRequest가 null인 경우 수동으로 객체 생성
        if (request == null || (request.getEmail() == null && email != null)) {
            request = AdminLoginRequest.builder()
                    .email(email)
                    .password(password)
                    .build();
            log.info(">> 수동으로 AdminLoginRequest 생성: 이메일={}", email);
        }

        // 유효성 검사 실패 시 로그인 페이지로 리다이렉트
        if (bindingResult.hasErrors()) {
            log.warn(">> 로그인 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            return "admin/login";
        }

        try {
            // 로그인 처리
            AdminLoginResponse loginResponse = adminService.login(request);

            // 토큰을 쿠키에 저장
            Cookie accessTokenCookie = new Cookie("accessToken", loginResponse.getAccessToken());
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(3600); // 1시간

            Cookie refreshTokenCookie = new Cookie("refreshToken", loginResponse.getRefreshToken());
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge(86400); // 24시간

            response.addCookie(accessTokenCookie);
            response.addCookie(refreshTokenCookie);

            log.info(">> 관리자 로그인 성공: 이메일={}", email);
            return "redirect:/admin/dashboard";

        } catch (AuthenticationFailedException e) {
            log.error(">> 관리자 로그인 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "admin/login";
        } catch (Exception e) {
            log.error(">> 관리자 로그인 중 예외 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "로그인 처리 중 오류가 발생했습니다.");
            return "admin/login";
        }
    }

    /**
     * 관리자 회원가입 페이지
     */
    @GetMapping("/signup")
    public String signupPage(Model model) {
        log.info(">> 관리자 회원가입 페이지 요청");
        model.addAttribute("adminSignupRequest", new AdminSignupRequest());
        return "admin/signup";
    }

    /**
     * 관리자 회원가입 처리
     */
    @PostMapping("/signup")
    public String signup(@ModelAttribute AdminSignupRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model,
                         @RequestParam(required = false) String email,
                         @RequestParam(required = false) String password,
                         @RequestParam(required = false) String name,
                         @RequestParam(required = false) Integer age,
                         @RequestParam(required = false) String sex,
                         @RequestParam(required = false) String phoneNum,
                         @RequestParam(required = false) String location) {

        log.info(">> 관리자 회원가입 요청: 이메일={}, 이름={}", email, name);

        // AdminSignupRequest가 null인 경우 수동으로 객체 생성
        if (request == null || (request.getEmail() == null && email != null)) {
            request = AdminSignupRequest.builder()
                    .email(email)
                    .password(password)
                    .name(name)
                    .age(age)
                    .sex(sex)
                    .phoneNum(phoneNum)
                    .location(location)
                    .build();
            log.info(">> 수동으로 AdminSignupRequest 생성: {}", request);
        }

        // 유효성 검사 실패 시 회원가입 페이지로 리다이렉트
        if (bindingResult.hasErrors()) {
            log.warn(">> 회원가입 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            return "admin/signup";
        }

        try {
            // 회원가입 처리
            log.info(">> AdminService.signup 호출 전");
            AdminSignupResponse signupResponse = adminService.signup(request);
            log.info(">> 관리자 회원가입 성공: id={}, 이메일={}", signupResponse.getId(), signupResponse.getEmail());

            redirectAttributes.addFlashAttribute("successMessage", "관리자 계정이 성공적으로 생성되었습니다. 로그인해주세요.");
            return "redirect:/admin/login";

        } catch (Exception e) {
            log.error(">> 관리자 회원가입 실패: {}", e.getMessage(), e);
            model.addAttribute("adminSignupRequest", request); // 입력값 유지
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/signup";
        }
    }

    /**
     * 관리자 로그아웃 처리
     */
    @GetMapping("/logout")
    @RoleRequired(UserRole.ADMIN)
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        log.info(">> 관리자 로그아웃 요청");

        // 쿠키에서 토큰 추출
        String accessToken = null;
        String refreshToken = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                } else if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        // 토큰이 있으면 로그아웃 처리
        if (accessToken != null && refreshToken != null) {
            try {
                adminService.logout(accessToken, refreshToken);
                log.info(">> 관리자 로그아웃 성공");
            } catch (Exception e) {
                log.error(">> 관리자 로그아웃 처리 중 오류 발생: {}", e.getMessage(), e);
            }
        } else {
            log.warn(">> 로그아웃 실패: 토큰 정보 없음");
        }

        // 쿠키 삭제
        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);

        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);

        return "redirect:/admin/login";
    }

    /**
     * 사용자 관리 페이지
     */
    @RoleRequired(UserRole.ADMIN)
    @GetMapping("/users")
    public String userManagement(Model model) {
        log.info(">> 사용자 관리 페이지 요청");

        try {
            // 계정 잠금 상태 동기화 먼저 수행
            int syncCount = adminService.syncUserLockStatus();
            log.info(">> 계정 잠금 상태 동기화 완료: {}개 계정", syncCount);
            
            // 동기화 후 모든 사용자 목록 조회
            Map<String, Object> usersData = adminService.getAllUsers();
            model.addAttribute("regularUsers", usersData.get("regularUsers"));
            model.addAttribute("snsUsers", usersData.get("snsUsers"));
            model.addAttribute("redisLockStatus", usersData.get("redisLockStatus"));
            model.addAttribute("redisLockRemainingTime", usersData.get("redisLockRemainingTime")); 
            model.addAttribute("roles", UserRole.values());
            
            // 동기화 결과 메시지 추가
            if (syncCount > 0) {
                model.addAttribute("syncMessage", String.format("%d개 계정의 잠금 상태가 동기화되었습니다.", syncCount));
            }

            log.info(">> 사용자 목록 조회 성공: 일반 사용자={}, SNS 사용자={}",
                    ((java.util.List<?>) usersData.get("regularUsers")).size(),
                    ((java.util.List<?>) usersData.get("snsUsers")).size());
        } catch (Exception e) {
            log.error(">> 사용자 목록 조회 실패: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "사용자 목록을 불러오는 데 실패했습니다.");
        }

        return "admin/users";
    }

    /**
     * 사용자 역할 변경 API
     */
    @RoleRequired(UserRole.ADMIN)
    @PutMapping("/api/users/{userType}/{userId}/role")
    @ResponseBody
    public ResponseEntity<RsData<ModifyRoleResponse>> modifyRole(
            @PathVariable String userType,
            @PathVariable Long userId,
            @Valid @RequestBody RoleUpdateRequest request,
            BindingResult bindingResult) {

        // 입력값 유효성 검사
        if (bindingResult.hasErrors()) {
            log.warn(">> 역할 변경 요청 유효성 검증 실패: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest()
                    .body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
        }

        log.info(">> 관리자 권한 변경 요청: userType={}, userId={}, role={}",
                userType, userId, request.getRole());

        // userType이 유효한지 검사
        if (!userType.equals("regular") && !userType.equals("sns")) {
            log.error(">> 유효하지 않은 사용자 타입: {}", userType);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RsData<>("400-BAD_REQUEST", "유효하지 않은 사용자 타입입니다. 'regular' 또는 'sns'여야 합니다."));
        }

        try {
            ModifyRoleResponse response = adminService.modifyRole(userType, userId, request.getRole());

            log.info(">> 권한 변경 성공: userType={}, userId={}, newRole={}",
                    userType, userId, response.getRole());

            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "권한 변경 성공", response));
        } catch (Exception e) {
            log.error(">> 권한 변경 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RsData<>("400-BAD_REQUEST", e.getMessage()));
        }
    }

    /**
     * 티켓 관리 페이지
     */
    @RoleRequired(UserRole.ADMIN)
    @GetMapping("/tickets")
    public String ticketManagement(Model model) {
        log.info(">> 티켓 관리 페이지 요청");

        try {
            // 모든 티켓 목록 조회
            List<org.codeNbug.mainserver.domain.admin.dto.response.TicketAdminDto> tickets = adminService.getAllTickets();
            model.addAttribute("tickets", tickets);

            log.info(">> 티켓 목록 조회 성공: {} 개의 티켓", tickets.size());
        } catch (Exception e) {
            log.error(">> 티켓 목록 조회 실패: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "티켓 목록을 불러오는 데 실패했습니다.");
        }

        return "admin/tickets";
    }

    /**
     * 이벤트 관리 페이지
     */
    @RoleRequired(UserRole.ADMIN)
    @GetMapping("/events")
    public String eventManagement(Model model) {
        log.info(">> 이벤트 관리 페이지 요청");

        try {
            // 모든 이벤트 목록 조회
            List<org.codeNbug.mainserver.domain.admin.dto.response.EventAdminDto> events = adminService.getAllEvents();
            model.addAttribute("events", events);

            log.info(">> 이벤트 목록 조회 성공: {} 개의 이벤트", events.size());
        } catch (Exception e) {
            log.error(">> 이벤트 목록 조회 실패: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "이벤트 목록을 불러오는 데 실패했습니다.");
        }

        return "admin/events";
    }

    // === ExceptionHandler 분리 ===
    // 뷰 반환용 (페이지 요청에서만 동작)
    @ExceptionHandler(Exception.class)
    public String handleViewException(Exception e, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        String uri = request.getRequestURI();
        // API 요청이 아닌 경우에만 동작
        if (!uri.contains("/api/")) {
            log.error(">> 예외 발생(뷰): {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "요청 처리 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/admin/login";
        }
        // API 요청이면 null 반환해서 아래 API용 핸들러로 위임
        return null;
    }

    // API용 (RestController, @ResponseBody에서만 동작)
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<RsData<Void>> handleApiException(Exception e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.contains("/api/")) {
            log.error(">> 예외 발생(API): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RsData.error("500-INTERNAL_SERVER_ERROR", "요청 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
        // 뷰 요청이면 null 반환해서 위 핸들러로 위임
        return null;
    }

    /**
     * 이벤트 상세 정보 페이지
     */
    @RoleRequired(UserRole.ADMIN)
    @GetMapping("/events/{id}")
    public String eventDetail(@PathVariable("id") Long eventId, Model model, RedirectAttributes redirectAttributes) {
        log.info(">> 이벤트 상세 정보 페이지 요청: id={}", eventId);

        try {
            // 특정 이벤트 조회
            org.codeNbug.mainserver.domain.admin.dto.response.EventAdminDto event = adminService.getEvent(eventId);
            model.addAttribute("event", event);

            // 해당 이벤트의 티켓 목록 조회 (옵션)
            List<org.codeNbug.mainserver.domain.admin.dto.response.TicketAdminDto> tickets =
                    adminService.getAllTickets().stream()
                            .filter(ticket -> ticket.getEventId().equals(eventId))
                            .collect(Collectors.toList());
            model.addAttribute("tickets", tickets);

            log.info(">> 이벤트 상세 정보 조회 성공: id={}, 티켓 수={}", eventId, tickets.size());

            return "admin/event-detail";
        } catch (Exception e) {
            log.error(">> 이벤트 상세 정보 조회 실패: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "이벤트 상세 정보를 불러오는 데 실패했습니다: " + e.getMessage());
            return "redirect:/admin/events";
        }
    }

    /**
     * 이벤트를 삭제합니다.
     * 관리자 권한으로 이벤트를 삭제하고, 관련된 구매자들에게 알림을 전송합니다.
     *
     * @param eventId 삭제할 이벤트 ID
     * @return 성공 메시지
     * @throws IllegalAccessException 이미 삭제된 이벤트인 경우
     */
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<RsData<String>> deleteEvent(@PathVariable Long eventId) throws IllegalAccessException {
        log.info(">> 이벤트 삭제 요청: eventId={}", eventId);
        adminService.deleteEvent(eventId);
        log.info(">> 이벤트 삭제 완료: eventId={}", eventId);
        return ResponseEntity.ok(RsData.success("이벤트가 성공적으로 삭제되었습니다."));
    }

    /**
     * 삭제 대기 중인 이벤트 목록을 조회합니다.
     */
    @GetMapping("/api/events/deleted")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<List<EventAdminDto>>> getDeletedEvents() {
        log.info(">> 삭제 대기 중인 이벤트 목록 조회 요청");
        
        try {
            List<EventAdminDto> events = adminService.getDeletedEvents();
            log.info(">> 삭제 대기 중인 이벤트 목록 조회 성공: {} 개의 이벤트", events.size());
            return ResponseEntity.ok(RsData.success("삭제 대기 중인 이벤트 목록 조회 성공", events));
        } catch (Exception e) {
            log.error(">> 삭제 대기 중인 이벤트 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RsData.error("500-INTERNAL_SERVER_ERROR", "삭제 대기 중인 이벤트 목록을 불러오는 데 실패했습니다."));
        }
    }

    /**
     * 삭제된 이벤트를 복구합니다.
     * 
     * @param eventId 복구할 이벤트 ID
     * @return 복구된 이벤트 정보
     */
    @PostMapping("/api/events/{eventId}/restore")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<EventAdminDto>> restoreEvent(
            @PathVariable Long eventId) {
        log.info(">> 이벤트 복구 요청: eventId={}", eventId);
        
        try {
            EventAdminDto event = adminService.restoreEvent(eventId);
            log.info(">> 이벤트 복구 성공: eventId={}", eventId);
            return ResponseEntity.ok(RsData.success("이벤트가 성공적으로 복구되었습니다.", event));
        } catch (IllegalAccessException e) {
            log.error(">> 이벤트 복구 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(RsData.error("400-BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error(">> 이벤트 복구 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RsData.error("500-INTERNAL_SERVER_ERROR", "이벤트 복구 중 오류가 발생했습니다."));
        }
    }

    /**
     * 이벤트 목록을 JSON으로 반환하는 API
     */
    @GetMapping(value = "/api/events", produces = "application/json")
    @ResponseBody
    public Map<String, Object> getEventListJson() {
        List<EventAdminDto> events = adminService.getAllEvents();
        Map<String, Object> result = new HashMap<>();
        result.put("events", events);
        return result;
    }

    /**
     * 모니터링(Grafana) 페이지
     */
    @GetMapping("/monitoring")
    @RoleRequired(UserRole.ADMIN)
    public String monitoringPage(Model model) {
        model.addAttribute("currentPage", "monitoring");
        return "admin/monitoring";
    }

    /**
     * 계정 만료일 연장 API
     */
    @PutMapping("/api/users/{userId}/extend-account")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<Void>> extendAccountExpiry(@PathVariable Long userId) {
        log.info(">> 계정 만료일 연장 요청: userId={}", userId);
        adminService.extendAccountExpiry(userId);
        return ResponseEntity.ok(RsData.success("계정 만료일이 성공적으로 연장되었습니다."));
    }

    /**
     * 비밀번호 만료일 연장 API
     */
    @PutMapping("/api/users/{userId}/extend-password")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<Void>> extendPasswordExpiry(@PathVariable Long userId) {
        log.info(">> 비밀번호 만료일 연장 요청: userId={}", userId);
        adminService.extendPasswordExpiry(userId);
        return ResponseEntity.ok(RsData.success("비밀번호 만료일이 성공적으로 연장되었습니다."));
    }

    /**
     * 계정 비활성화 API
     */
    @PutMapping("/api/users/{userId}/disable")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<Void>> disableAccount(@PathVariable Long userId) {
        log.info(">> 계정 비활성화 요청: userId={}", userId);
        adminService.disableAccount(userId);
        return ResponseEntity.ok(RsData.success("계정이 성공적으로 비활성화되었습니다."));
    }

    /**
     * 계정 활성화 API
     */
    @PutMapping("/api/users/{userId}/enable")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<Void>> enableAccount(@PathVariable Long userId) {
        log.info(">> 계정 활성화 요청: userId={}", userId);
        adminService.enableAccount(userId);
        return ResponseEntity.ok(RsData.success("계정이 성공적으로 활성화되었습니다."));
    }

    /**
     * 계정 잠금 해제 API
     */
    @PutMapping("/api/users/{userId}/unlock")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<Void>> unlockAccount(@PathVariable Long userId) {
        log.info(">> 계정 잠금 해제 요청: userId={}", userId);
        adminService.unlockAccount(userId);
        return ResponseEntity.ok(RsData.success("계정이 성공적으로 잠금 해제되었습니다."));
    }

    /**
     * 관리자 설정 페이지
     */
    @RoleRequired(UserRole.ADMIN)
    @GetMapping("/settings")
    public String settingsPage(Model model) {
        log.info(">> 관리자 설정 페이지 요청");
        try {
            // 계정 잠금 상태 동기화 먼저 수행
            int syncCount = adminService.syncUserLockStatus();
            log.info(">> 계정 잠금 상태 동기화 완료: {}개 계정", syncCount);
            
            // 동기화 후 모든 사용자 목록 조회
            Map<String, Object> usersData = adminService.getAllUsers();
            model.addAttribute("regularUsers", usersData.get("regularUsers"));
            model.addAttribute("snsUsers", usersData.get("snsUsers"));
            model.addAttribute("redisLockStatus", usersData.get("redisLockStatus"));
            model.addAttribute("redisLockRemainingTime", usersData.get("redisLockRemainingTime"));
            
            // 동기화 결과 메시지 추가
            if (syncCount > 0) {
                model.addAttribute("syncMessage", String.format("%d개 계정의 잠금 상태가 동기화되었습니다.", syncCount));
            }
        } catch (Exception e) {
            log.error(">> 설정 페이지 사용자 목록 조회 실패: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "사용자 목록을 불러오는 데 실패했습니다.");
        }
        return "admin/settings";
    }

    /**
     * 모든 사용자의 로그인 시도 횟수 초기화 API
     * 관리자만 사용 가능한 기능입니다.
     */
    @PostMapping("/api/users/reset-login-attempts")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<Map<String, Integer>>> resetAllLoginAttemptCounts() {
        log.info(">> 모든 사용자 로그인 시도 횟수 초기화 요청");
        
        try {
            int updatedCount = adminService.resetAllLoginAttemptCounts();
            
            Map<String, Integer> result = new HashMap<>();
            result.put("updatedCount", updatedCount);
            
            log.info(">> 로그인 시도 횟수 초기화 완료: {}개 계정", updatedCount);
            return ResponseEntity.ok(RsData.success("모든 사용자의 로그인 시도 횟수가 초기화되었습니다.", result));
        } catch (Exception e) {
            log.error(">> 로그인 시도 횟수 초기화 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RsData.error("500-INTERNAL_SERVER_ERROR", "로그인 시도 횟수 초기화 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 모든 사용자의 계정 잠금 상태 동기화 API
     * 관리자만 사용 가능한 기능입니다.
     */
    @PostMapping("/api/users/sync-lock-status")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<Map<String, Integer>>> syncUserLockStatus() {
        log.info(">> 계정 잠금 상태 동기화 요청");
        
        try {
            int syncCount = adminService.syncUserLockStatus();
            
            Map<String, Integer> result = new HashMap<>();
            result.put("syncCount", syncCount);
            
            log.info(">> 계정 잠금 상태 동기화 완료: {}개 계정", syncCount);
            return ResponseEntity.ok(RsData.success("계정 잠금 상태 동기화가 완료되었습니다.", result));
        } catch (Exception e) {
            log.error(">> 계정 잠금 상태 동기화 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RsData.error("500-INTERNAL_SERVER_ERROR", "계정 잠금 상태 동기화 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 여러 계정을 동시에 활성화하는 API
     * 관리자만 사용 가능한 기능입니다.
     */
    @PostMapping("/api/users/bulk/enable")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<Map<String, Integer>>> bulkEnableAccounts(
            @RequestBody @Valid BulkUserIdsRequest request,
            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            log.warn(">> 일괄 계정 활성화 요청 유효성 검증 실패: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest()
                    .body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
        }
        
        log.info(">> 일괄 계정 활성화 요청: 총 {}개 계정", request.getUserIds().size());
        
        try {
            int processedCount = adminService.bulkEnableAccounts(request.getUserIds());
            
            Map<String, Integer> result = new HashMap<>();
            result.put("processedCount", processedCount);
            
            log.info(">> 일괄 계정 활성화 완료: {}개 계정", processedCount);
            return ResponseEntity.ok(RsData.success("선택한 계정이 성공적으로 활성화되었습니다.", result));
        } catch (Exception e) {
            log.error(">> 일괄 계정 활성화 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RsData.error("500-INTERNAL_SERVER_ERROR", "계정 활성화 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 여러 계정을 동시에 비활성화하는 API
     * 관리자만 사용 가능한 기능입니다.
     */
    @PostMapping("/api/users/bulk/disable")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<Map<String, Integer>>> bulkDisableAccounts(
            @RequestBody @Valid BulkUserIdsRequest request,
            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            log.warn(">> 일괄 계정 비활성화 요청 유효성 검증 실패: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest()
                    .body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
        }
        
        log.info(">> 일괄 계정 비활성화 요청: 총 {}개 계정", request.getUserIds().size());
        
        try {
            int processedCount = adminService.bulkDisableAccounts(request.getUserIds());
            
            Map<String, Integer> result = new HashMap<>();
            result.put("processedCount", processedCount);
            
            log.info(">> 일괄 계정 비활성화 완료: {}개 계정", processedCount);
            return ResponseEntity.ok(RsData.success("선택한 계정이 성공적으로 비활성화되었습니다.", result));
        } catch (Exception e) {
            log.error(">> 일괄 계정 비활성화 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RsData.error("500-INTERNAL_SERVER_ERROR", "계정 비활성화 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 여러 계정을 동시에 잠그는 API
     * 관리자만 사용 가능한 기능입니다.
     */
    @PostMapping("/api/users/bulk/lock")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<Map<String, Integer>>> bulkLockAccounts(
            @RequestBody @Valid BulkUserIdsRequest request,
            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            log.warn(">> 일괄 계정 잠금 요청 유효성 검증 실패: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest()
                    .body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
        }
        
        log.info(">> 일괄 계정 잠금 요청: 총 {}개 계정", request.getUserIds().size());
        
        try {
            int processedCount = adminService.bulkLockAccounts(request.getUserIds());
            
            Map<String, Integer> result = new HashMap<>();
            result.put("processedCount", processedCount);
            
            log.info(">> 일괄 계정 잠금 완료: {}개 계정", processedCount);
            return ResponseEntity.ok(RsData.success("선택한 계정이 성공적으로 잠겼습니다.", result));
        } catch (Exception e) {
            log.error(">> 일괄 계정 잠금 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RsData.error("500-INTERNAL_SERVER_ERROR", "계정 잠금 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 여러 계정을 동시에 잠금 해제하는 API
     * 관리자만 사용 가능한 기능입니다.
     */
    @PostMapping("/api/users/bulk/unlock")
    @RoleRequired(UserRole.ADMIN)
    public ResponseEntity<RsData<Map<String, Integer>>> bulkUnlockAccounts(
            @RequestBody @Valid BulkUserIdsRequest request,
            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            log.warn(">> 일괄 계정 잠금 해제 요청 유효성 검증 실패: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest()
                    .body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
        }
        
        log.info(">> 일괄 계정 잠금 해제 요청: 총 {}개 계정", request.getUserIds().size());
        
        try {
            int processedCount = adminService.bulkUnlockAccounts(request.getUserIds());
            
            Map<String, Integer> result = new HashMap<>();
            result.put("processedCount", processedCount);
            
            log.info(">> 일괄 계정 잠금 해제 완료: {}개 계정", processedCount);
            return ResponseEntity.ok(RsData.success("선택한 계정이 성공적으로 잠금 해제되었습니다.", result));
        } catch (Exception e) {
            log.error(">> 일괄 계정 잠금 해제 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RsData.error("500-INTERNAL_SERVER_ERROR", "계정 잠금 해제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}