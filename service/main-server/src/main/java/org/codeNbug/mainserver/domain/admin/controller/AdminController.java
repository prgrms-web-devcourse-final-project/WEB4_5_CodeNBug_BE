package org.codeNbug.mainserver.domain.admin.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeNbug.mainserver.domain.admin.dto.request.AdminLoginRequest;
import org.codeNbug.mainserver.domain.admin.dto.request.AdminSignupRequest;
import org.codeNbug.mainserver.domain.admin.dto.request.RoleUpdateRequest;
import org.codeNbug.mainserver.domain.admin.dto.response.AdminLoginResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.AdminSignupResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.DashboardStatsResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.ModifyRoleResponse;
import org.codeNbug.mainserver.domain.admin.service.AdminService;
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
            // 모든 사용자 목록 조회
            Map<String, Object> usersData = adminService.getAllUsers();
            model.addAttribute("regularUsers", usersData.get("regularUsers"));
            model.addAttribute("snsUsers", usersData.get("snsUsers"));
            model.addAttribute("roles", UserRole.values());

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

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, RedirectAttributes redirectAttributes) {
        log.error(">> 예외 발생: {}", e.getMessage(), e);
        redirectAttributes.addFlashAttribute("errorMessage", "요청 처리 중 오류가 발생했습니다: " + e.getMessage());
        return "redirect:/admin/login";
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handleException(Exception e) {
        log.error(">> 예외 발생: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RsData.error("500-INTERNAL_SERVER_ERROR", "요청 처리 중 오류가 발생했습니다: " + e.getMessage()));
    }
}