package org.codeNbug.mainserver.domain.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeNbug.mainserver.domain.purchase.dto.PurchaseHistoryDetailResponse;
import org.codeNbug.mainserver.domain.purchase.dto.PurchaseHistoryListResponse;
import org.codeNbug.mainserver.domain.purchase.service.PurchaseService;
import org.codeNbug.mainserver.domain.user.dto.request.LoginRequest;
import org.codeNbug.mainserver.domain.user.dto.request.SignupRequest;
import org.codeNbug.mainserver.domain.user.dto.request.UserUpdateRequest;
import org.codeNbug.mainserver.domain.user.dto.response.LoginResponse;
import org.codeNbug.mainserver.domain.user.dto.response.SignupResponse;
import org.codeNbug.mainserver.domain.user.dto.response.UserProfileResponse;
import org.codeNbug.mainserver.domain.user.service.UserService;
import org.codeNbug.mainserver.global.Redis.service.TokenService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codeNbug.mainserver.global.exception.globalException.DuplicateEmailException;
import org.codeNbug.mainserver.global.exception.security.AuthenticationFailedException;
import org.codeNbug.mainserver.global.util.CookieUtil;
import org.codeNbug.mainserver.global.util.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 관련 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {
    private final UserService userService;
    private final CookieUtil cookieUtil;
    private final TokenService tokenService;
    private final PurchaseService purchaseService;

    /**
     * 회원가입 API
     *
     * @param request       회원가입 요청 정보
     * @param bindingResult 유효성 검사 결과
     * @return API 응답
     */
    @PostMapping("/signup")
    public ResponseEntity<RsData<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            BindingResult bindingResult) {

        // 입력값 유효성 검사
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
        }

        try {
            // 회원가입 처리
            SignupResponse response = userService.signup(request);
            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "회원가입 성공", response));
        } catch (DuplicateEmailException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RsData<>("409-CONFLICT", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 로그인 API
     *
     * @param request       로그인 요청 정보
     * @param bindingResult 유효성 검사 결과
     * @param response      HTTP 응답 객체 (쿠키 설정용)
     * @return API 응답
     */
    @PostMapping("/login")
    public ResponseEntity<RsData<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            BindingResult bindingResult,
            HttpServletResponse response) {

        // 입력값 유효성 검사
        if (bindingResult.hasErrors()) {
            log.warn(">> 로그인 요청 유효성 검증 실패: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest()
                    .body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
        }

        try {
            log.info(">> 로그인 시도: 이메일={}", request.getEmail());
            
            // 로그인 처리
            LoginResponse loginResponse = userService.login(request);
            log.info(">> 로그인 성공: 이메일={}, 토큰 발급 완료", request.getEmail());
            log.debug(">> 발급된 액세스 토큰: {}", loginResponse.getAccessToken());
            log.debug(">> 발급된 리프레시 토큰: {}", loginResponse.getRefreshToken());

            // 쿠키에 토큰 설정
            cookieUtil.setAccessTokenCookie(response, loginResponse.getAccessToken());
            cookieUtil.setRefreshTokenCookie(response, loginResponse.getRefreshToken());
            log.info(">> 쿠키에 토큰 설정 완료");

            // 응답 본문에서는 토큰 정보 제외
            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "로그인 성공", LoginResponse.ofTokenTypeOnly()));

        } catch (AuthenticationFailedException e) {
            log.warn(">> 로그인 인증 실패: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>("401-UNAUTHORIZED", e.getMessage()));
        } catch (Exception e) {
            log.error(">> 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 로그아웃 API
     * 일반 회원가입 사용자와 SNS 로그인 사용자 모두 지원합니다.
     *
     * @param request  HTTP 요청 객체 (쿠키 추출용)
     * @param response HTTP 응답 객체 (쿠키 삭제용)
     * @return API 응답
     */
    @PostMapping("/logout")
    public ResponseEntity<RsData<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // 디버깅 로그 추가
            log.info(">> 로그아웃 요청 시작");
            
            // 헤더에서 토큰 추출 시도
            String authHeader = request.getHeader("Authorization");
            String accessToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
                log.info(">> 헤더에서 액세스 토큰 추출: {}", accessToken);
            }

            // 헤더에 토큰이 없으면 쿠키에서 추출 시도
            if (accessToken == null) {
                accessToken = cookieUtil.getAccessTokenFromCookie(request);
                log.info(">> 쿠키에서 액세스 토큰 추출: {}", accessToken);
            }

            String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);
            log.info(">> 쿠키에서 리프레시 토큰 추출: {}", refreshToken);

            // 토큰 추출 실패 시 응답 변경 (더 자세한 에러 메시지)
            if (accessToken == null || refreshToken == null) {
                log.warn(">> 토큰 추출 실패: accessToken={}, refreshToken={}", 
                        accessToken != null ? "있음" : "없음", 
                        refreshToken != null ? "있음" : "없음");
                
                // 쿠키 삭제 (있는 경우만)
                cookieUtil.deleteAccessTokenCookie(response);
                cookieUtil.deleteRefreshTokenCookie(response);
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new RsData<>("401-UNAUTHORIZED", "인증 정보가 필요합니다. 다시 로그인해주세요."));
            }

            try {
                // 로그아웃 처리 (일반 사용자 및 SNS 사용자 모두 지원)
                userService.logout(accessToken, refreshToken);
                log.info(">> 로그아웃 처리 성공");
            } catch (Exception e) {
                log.error(">> 로그아웃 처리 중 오류 발생: {}", e.getMessage());
                // 오류가 발생해도 클라이언트의 쿠키는 삭제
            }

            // 쿠키 삭제
            cookieUtil.deleteAccessTokenCookie(response);
            cookieUtil.deleteRefreshTokenCookie(response);
            log.info(">> 쿠키 삭제 완료");

            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "로그아웃 성공"));
        } catch (Exception e) {
            log.error(">> 로그아웃 처리 중 예외 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 회원 탈퇴 API
     *
     * @param request  HTTP 요청 객체 (쿠키 추출용)
     * @param response HTTP 응답 객체 (쿠키 삭제용)
     * @return API 응답
     */
    @DeleteMapping("/me")
    public ResponseEntity<RsData<Void>> withdrawUser(
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // 헤더에서 토큰 추출 시도
            String authHeader = request.getHeader("Authorization");
            String accessToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            }

            // 헤더에 토큰이 없으면 쿠키에서 추출 시도
            if (accessToken == null) {
                accessToken = cookieUtil.getAccessTokenFromCookie(request);
            }

            String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);

            if (accessToken == null || refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new RsData<>("401-UNAUTHORIZED", "인증 정보가 필요합니다."));
            }

            // 토큰에서 식별자(이메일 또는 소셜ID) 추출
            String email = tokenService.getSubjectFromToken(accessToken);

            // 회원 탈퇴 처리
            userService.withdrawUser(email, accessToken, refreshToken);

            // 쿠키 삭제
            cookieUtil.deleteAccessTokenCookie(response);
            cookieUtil.deleteRefreshTokenCookie(response);

            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "회원 탈퇴 성공"));
        } catch (AuthenticationFailedException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>("401-UNAUTHORIZED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RsData<>("404-NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회합니다.
     * 일반 사용자와 SNS 사용자 모두 지원합니다.
     *
     * @return ResponseEntity<RsData < UserProfileResponse>> 프로필 정보 응답
     */
    @GetMapping("/me")
    public ResponseEntity<RsData<UserProfileResponse>> getProfile() {
        try {
            log.info(">> 사용자 프로필 조회 요청");
            
            UserProfileResponse profile = userService.getProfile();
            
            log.info(">> 프로필 조회 성공: userId={}, isSnsUser={}", 
                    profile.getId(), profile.getIsSnsUser());
            
            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "프로필 조회 성공", profile));
        } catch (AuthenticationFailedException e) {
            log.warn(">> 프로필 조회 인증 실패: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>("401-UNAUTHORIZED", e.getMessage()));
        } catch (Exception e) {
            log.error(">> 프로필 조회 중 오류 발생: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보를 수정합니다.
     * 일반 사용자와 SNS 사용자 모두 지원합니다.
     *
     * @param request 수정할 프로필 정보
     * @return ResponseEntity<RsData < UserProfileResponse>> 수정된 프로필 정보 응답
     */
    @PutMapping("/me")
    public ResponseEntity<RsData<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UserUpdateRequest request) {
        try {
            log.info(">> 사용자 프로필 수정 요청: name={}, phoneNum={}, location={}", 
                    request.getName(), request.getPhoneNum(), request.getLocation());
            
            // 프로필 수정 처리
            UserProfileResponse response = userService.updateProfile(request);
            
            log.info(">> 프로필 수정 성공: userId={}, isSnsUser={}", 
                    response.getId(), response.getIsSnsUser());
            
            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "프로필 수정 성공", response));
        } catch (AuthenticationFailedException e) {
            log.warn(">> 프로필 수정 인증 실패: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>("401-UNAUTHORIZED", "인증이 필요합니다."));
        } catch (Exception e) {
            log.error(">> 프로필 수정 중 오류 발생: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 사용자의 구매 이력 목록을 조회합니다.
     * 일반 사용자와 SNS 사용자 모두 지원합니다.
     *
     * @return ResponseEntity<RsData<PurchaseHistoryListResponse>> 구매 이력 목록 응답
     */
    @GetMapping("/me/purchases")
    public ResponseEntity<RsData<PurchaseHistoryListResponse>> getPurchaseHistoryList() {
        try {
            log.info(">> 사용자 구매 이력 조회 요청");
            
            Long userId = SecurityUtil.getCurrentUserId();
            PurchaseHistoryListResponse response = purchaseService.getPurchaseHistoryList(userId);
            
            log.info(">> 구매 이력 조회 성공: userId={}, 조회된 건수={}", 
                    userId, response.getPurchases().size());
            
            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "구매 이력 조회 성공", response));
        } catch (AuthenticationFailedException e) {
            log.warn(">> 구매 이력 조회 인증 실패: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>("401-UNAUTHORIZED", "로그인이 필요합니다."));
        } catch (Exception e) {
            log.error(">> 구매 이력 조회 중 오류 발생: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 사용자의 특정 구매 이력 상세 정보를 조회합니다.
     *
     * @param purchaseId 구매 ID
     * @return ResponseEntity<RsData<PurchaseHistoryDetailResponse>> 구매 이력 상세 응답
     */
    @GetMapping("/me/purchases/{purchaseId}")
    public ResponseEntity<RsData<PurchaseHistoryDetailResponse>> getPurchaseHistoryDetail(
            @PathVariable Long purchaseId) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            PurchaseHistoryDetailResponse response = purchaseService.getPurchaseHistoryDetail(userId, purchaseId);
            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "구매 이력 상세 조회 성공", response));
        } catch (AuthenticationFailedException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>("401-UNAUTHORIZED", "로그인이 필요합니다."));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RsData<>("400-BAD_REQUEST", e.getMessage()));
        }
    }
}
