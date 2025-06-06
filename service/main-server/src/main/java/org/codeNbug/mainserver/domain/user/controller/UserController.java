package org.codeNbug.mainserver.domain.user.controller;

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
import org.codeNbug.mainserver.global.dto.RsData;
import org.codeNbug.mainserver.global.util.SecurityUtil;
import org.codenbug.common.util.CookieUtil;
import org.codenbug.user.redis.service.TokenService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 관련 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
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

		// 회원가입 처리
		SignupResponse response = userService.signup(request);
		return ResponseEntity.ok(
			new RsData<>("200-SUCCESS", "회원가입 성공", response));
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
		@RequestParam(required = false) String domain,
		BindingResult bindingResult,
		HttpServletResponse response) {

		// 입력값 유효성 검사
		if (bindingResult.hasErrors()) {
			log.warn(">> 로그인 요청 유효성 검증 실패: {}", bindingResult.getAllErrors());
			return ResponseEntity.badRequest()
				.body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
		}

		log.info(">> 로그인 시도: 이메일={}", request.getEmail());

		try {
			// 로그인 처리
			LoginResponse loginResponse = userService.login(request);
			log.info(">> 로그인 성공: 이메일={}, 토큰 발급 완료", request.getEmail());

			if (domain != null) {
				cookieUtil.setAccessTokenCookie(response, domain, loginResponse.getAccessToken());
				cookieUtil.setRefreshTokenCookie(response, domain, loginResponse.getRefreshToken());
			}
			// 쿠키에 토큰 설정
			cookieUtil.setAccessTokenCookie(response, loginResponse.getAccessToken());
			cookieUtil.setRefreshTokenCookie(response, loginResponse.getRefreshToken());
			log.info(">> 쿠키에 토큰 설정 완료");

			// 응답 본문에서는 토큰 정보 제외
			return ResponseEntity.ok(
				new RsData<>("200-SUCCESS", "로그인 성공", LoginResponse.ofTokenTypeOnly()));
		} catch (Exception e) {
			log.error(">> 로그인 실패: 이메일={}, 오류={}", request.getEmail(), e.getMessage());
			throw e; // 예외 다시 발생시켜 글로벌 예외 핸들러에서 처리하도록 함
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

			// 쿠키 삭제 - 브라우저의 기존 쿠키를 직접 만료 처리
			cookieUtil.expireAuthCookies(request, response);

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new RsData<>("401-UNAUTHORIZED", "인증 정보가 필요합니다. 다시 로그인해주세요."));
		}

		// 로그아웃 처리 (일반 사용자 및 SNS 사용자 모두 지원)
		userService.logout(accessToken, refreshToken);
		log.info(">> 로그아웃 처리 성공");

		// 쿠키 삭제 - 브라우저의 기존 쿠키를 직접 만료 처리
		cookieUtil.expireAuthCookies(request, response);
		log.info(">> 쿠키 삭제 완료");

		return ResponseEntity.ok(
			new RsData<>("200-SUCCESS", "로그아웃 성공"));
	}

	/**
	 * 회원 탈퇴 API
	 * 일반 사용자와 SNS 사용자 모두 지원합니다.
	 *
	 * @param request  HTTP 요청 객체 (쿠키 추출용)
	 * @param response HTTP 응답 객체 (쿠키 삭제용)
	 * @return API 응답
	 */
	@DeleteMapping("/me")
	public ResponseEntity<RsData<Void>> withdrawUser(
		HttpServletRequest request,
		HttpServletResponse response) {
		log.info(">> 회원 탈퇴 요청 시작");

		// 헤더에서 토큰 추출 시도
		String authHeader = request.getHeader("Authorization");
		String accessToken = null;
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			accessToken = authHeader.substring(7);
			log.debug(">> 헤더에서 액세스 토큰 추출 성공");
		}

		// 헤더에 토큰이 없으면 쿠키에서 추출 시도
		if (accessToken == null) {
			accessToken = cookieUtil.getAccessTokenFromCookie(request);
			log.debug(">> 쿠키에서 액세스 토큰 추출: {}",
				accessToken != null ? "성공" : "실패");
		}

		String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);
		log.debug(">> 쿠키에서 리프레시 토큰 추출: {}",
			refreshToken != null ? "성공" : "실패");

		if (accessToken == null || refreshToken == null) {
			log.warn(">> 회원 탈퇴 실패: 인증 정보 부족");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new RsData<>("401-UNAUTHORIZED", "인증 정보가 필요합니다."));
		}

		// 토큰에서 식별자(이메일 또는 소셜ID:provider) 추출
		String identifier = tokenService.getSubjectFromToken(accessToken);
		log.info(">> 토큰에서 사용자 식별자 추출: {}", identifier);

		// 회원 탈퇴 처리
		userService.withdrawUser(identifier, accessToken, refreshToken);
		log.info(">> 회원 탈퇴 처리 완료: identifier={}", identifier);

		// 쿠키 삭제
		cookieUtil.expireAuthCookies(request, response);
		log.info(">> 인증 쿠키 삭제 완료");

		return ResponseEntity.ok(
			new RsData<>("200-SUCCESS", "회원 탈퇴 성공"));
	}

	/**
	 * 현재 로그인한 사용자의 프로필 정보를 조회합니다.
	 * 일반 사용자와 SNS 사용자 모두 지원합니다.
	 *
	 * @return ResponseEntity<RsData < UserProfileResponse>> 프로필 정보 응답
	 */
	@GetMapping("/me")
	public ResponseEntity<RsData<UserProfileResponse>> getProfile() {
		log.info(">> 사용자 프로필 조회 요청");

		UserProfileResponse profile = userService.getProfile();

		log.info(">> 프로필 조회 성공: userId={}, isSnsUser={}",
			profile.getId(), profile.getIsSnsUser());

		return ResponseEntity.ok(
			new RsData<>("200-SUCCESS", "프로필 조회 성공", profile));
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
		log.info(">> 사용자 프로필 수정 요청: name={}, age={}, sex={}, phoneNum={}, location={}",
			request.getName(), request.getAge(), request.getSex(), request.getPhoneNum(), request.getLocation());

		// 프로필 수정 처리
		UserProfileResponse response = userService.updateProfile(request);

		log.info(">> 프로필 수정 성공: userId={}, isSnsUser={}",
			response.getId(), response.getIsSnsUser());

		return ResponseEntity.ok(
			new RsData<>("200-SUCCESS", "프로필 수정 성공", response));
	}

	/**
	 * 사용자의 구매 이력 목록을 조회합니다.
	 * 일반 사용자와 SNS 사용자 모두 지원합니다.
	 *
	 * @param page 페이지 번호 (0부터 시작)
	 * @param size 페이지 크기
	 * @return ResponseEntity<RsData < PurchaseHistoryListResponse>> 구매 이력 목록 응답
	 */
	@GetMapping("/me/purchases")
	public ResponseEntity<RsData<PurchaseHistoryListResponse>> getPurchaseHistoryList(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size) {
		log.info(">> 사용자 구매 이력 조회 요청: page={}, size={}", page, size);

		Long userId = SecurityUtil.getCurrentUserId();
		Pageable pageable = PageRequest.of(page, size, Sort.by("purchaseDate").descending());
		PurchaseHistoryListResponse response = purchaseService.getPurchaseHistoryList(userId, pageable);

		log.info(">> 구매 이력 조회 성공: userId={}, 조회된 건수={}, 전체 건수={}",
			userId, response.getPurchases().size(), response.getTotalElements());

		return ResponseEntity.ok(
			new RsData<>("200-SUCCESS", "구매 이력 조회 성공", response));
	}

	/**
	 * 사용자의 특정 구매 이력 상세 정보를 조회합니다.
	 *
	 * @param purchaseId 구매 ID
	 * @return ResponseEntity<RsData < PurchaseHistoryDetailResponse>> 구매 이력 상세 응답
	 */
	@GetMapping("/me/purchases/{purchaseId}")
	public ResponseEntity<RsData<PurchaseHistoryDetailResponse>> getPurchaseHistoryDetail(
		@PathVariable Long purchaseId) {
		Long userId = SecurityUtil.getCurrentUserId();
		PurchaseHistoryDetailResponse response = purchaseService.getPurchaseHistoryDetail(userId, purchaseId);
		return ResponseEntity.ok(
			new RsData<>("200-SUCCESS", "구매 이력 상세 조회 성공", response));
	}
}
