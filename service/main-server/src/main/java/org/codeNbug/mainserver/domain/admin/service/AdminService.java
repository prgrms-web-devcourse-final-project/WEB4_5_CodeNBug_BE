package org.codeNbug.mainserver.domain.admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codeNbug.mainserver.domain.admin.dto.request.AdminLoginRequest;
import org.codeNbug.mainserver.domain.admin.dto.request.AdminSignupRequest;
import org.codeNbug.mainserver.domain.admin.dto.response.AdminLoginResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.AdminSignupResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.DashboardStatsResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.EventAdminDto;
import org.codeNbug.mainserver.domain.admin.dto.response.ModifyRoleResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.TicketAdminDto;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.notification.entity.NotificationEnum;
import org.codeNbug.mainserver.domain.notification.service.NotificationService;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.ticket.repository.TicketRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codeNbug.mainserver.global.exception.globalException.DuplicateEmailException;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.redis.service.TokenService;
import org.codenbug.user.security.exception.AuthenticationFailedException;
import org.codenbug.user.sns.Entity.SnsUser;
import org.codenbug.user.sns.repository.SnsUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자 관련 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

	private final UserRepository userRepository;
	private final SnsUserRepository snsUserRepository;
	private final EventRepository eventRepository;
	private final TicketRepository ticketRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final NotificationService notificationService;
	private final PurchaseRepository purchaseRepository;

	/**
	 * 관리자 회원가입 서비스
	 *
	 * @param request 관리자 회원가입 요청 정보
	 * @return 관리자 회원가입 응답 정보
	 * @throws DuplicateEmailException 이메일이 이미 존재하는 경우 발생하는 예외
	 */
	@Transactional
	public AdminSignupResponse signup(AdminSignupRequest request) {
		log.debug(">> AdminService.signup 메소드 시작: 이메일={}", request.getEmail());

		// 이메일 중복 확인
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateEmailException("이미 존재하는 이메일입니다.");
		}

		try {
			log.debug(">> 관리자 회원가입 처리 시작: 이메일={}", request.getEmail());

			// 사용자 엔티티 생성 및 저장 (ROLE_ADMIN으로 설정)
			User admin = request.toEntity(passwordEncoder);
			log.debug(">> User 엔티티 생성 완료: {}", admin);

			User savedAdmin = userRepository.save(admin);
			log.debug(">> User 엔티티 저장 완료: id={}", savedAdmin.getUserId());

			log.debug(">> 관리자 회원가입 완료: userId={}, email={}", savedAdmin.getUserId(), savedAdmin.getEmail());

			// 응답 반환
			AdminSignupResponse response = AdminSignupResponse.fromEntity(savedAdmin);
			log.debug(">> AdminSignupResponse 생성 완료");
			return response;
		} catch (Exception e) {
			log.error(">> 관리자 회원가입 처리 중 예외 발생: {}", e.getMessage(), e);
			throw new RuntimeException("관리자 회원가입 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	/**
	 * 관리자 로그인 서비스
	 *
	 * @param request 관리자 로그인 요청 정보
	 * @return 관리자 로그인 응답 정보 (JWT 토큰 포함)
	 * @throws AuthenticationFailedException 인증 실패 시 발생하는 예외
	 */
	@Transactional(readOnly = true)
	public AdminLoginResponse login(AdminLoginRequest request) {
		log.debug(">> 관리자 로그인 서비스 호출: 이메일={}", request.getEmail());

		// 사용자 조회
		User user = userRepository.findByEmail(request.getEmail())
			.orElseThrow(() -> {
				log.error(">> 관리자 로그인 실패: 존재하지 않는 이메일 - {}", request.getEmail());
				return new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요.");
			});

		// 관리자 권한 확인
		if (!"ROLE_ADMIN".equals(user.getRole())) {
			log.error(">> 관리자 로그인 실패: 관리자 권한 없음 - 이메일={}, 현재 권한={}", request.getEmail(), user.getRole());
			throw new AuthenticationFailedException("관리자 권한이 없습니다.");
		}

		// 비밀번호 검증
		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			log.error(">> 관리자 로그인 실패: 잘못된 비밀번호 - 이메일={}", request.getEmail());
			throw new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요.");
		}

		log.debug(">> 관리자 인증 성공: 이메일={}, userId={}", user.getEmail(), user.getUserId());

		// 토큰 생성
		TokenService.TokenInfo tokenInfo = tokenService.generateTokens(user.getEmail());
		log.debug(">> 토큰 생성 완료: 이메일={}", user.getEmail());

		// 응답 반환
		return AdminLoginResponse.of(tokenInfo.getAccessToken(), tokenInfo.getRefreshToken());
	}

	/**
	 * 관리자 로그아웃 처리
	 *
	 * @param accessToken  액세스 토큰
	 * @param refreshToken 리프레시 토큰
	 */
	@Transactional
	public void logout(String accessToken, String refreshToken) {
		log.debug(">> 관리자 로그아웃 서비스 호출");

		if (accessToken == null || refreshToken == null) {
			log.error(">> 관리자 로그아웃 실패: 토큰 값이 null");
			throw new AuthenticationFailedException("인증 정보가 필요합니다.");
		}

		try {
			// RefreshToken에서 subject(식별자) 추출
			String identifier = tokenService.getSubjectFromToken(refreshToken);
			log.debug(">> RefreshToken에서 식별자 추출 성공: {}", identifier);

			// AccessToken에서 subject(식별자) 추출 및 RefreshToken과 일치 여부 확인
			String accessTokenIdentifier = tokenService.getSubjectFromToken(accessToken);
			if (!identifier.equals(accessTokenIdentifier)) {
				log.error(">> 토큰 불일치: 액세스 토큰 식별자={}, 리프레시 토큰 식별자={}", accessTokenIdentifier, identifier);
				throw new AuthenticationFailedException("액세스 토큰과 리프레시 토큰의 사용자 정보가 일치하지 않습니다.");
			}

			// RefreshToken 삭제
			tokenService.deleteRefreshToken(identifier);
			log.debug(">> RefreshToken 삭제 완료: {}", identifier);

			// AccessToken 블랙리스트 처리
			long expirationTime = tokenService.getExpirationTimeFromToken(accessToken);
			tokenService.addToBlacklist(accessToken, expirationTime);
			log.debug(">> AccessToken 블랙리스트 처리 완료: 만료시간={}", expirationTime);

			log.debug(">> 관리자 로그아웃 처리 완료: 사용자={}", identifier);
		} catch (Exception e) {
			log.error(">> 관리자 로그아웃 처리 실패: {}", e.getMessage(), e);
			throw new AuthenticationFailedException("로그아웃 처리 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	/**
	 * 대시보드 통계 정보를 조회합니다.
	 * 사용자 수, 이벤트 수, 기타 통계 정보를 포함합니다.
	 *
	 * @return 대시보드 통계 정보 응답
	 */
	@Transactional(readOnly = true)
	public DashboardStatsResponse getDashboardStats() {
		log.debug(">> 대시보드 통계 정보 조회");

		try {
			// 일반 사용자 수 조회
			long regularUserCount = userRepository.count();
			log.debug(">> 일반 사용자 수: {}", regularUserCount);

			// SNS 사용자 수 조회
			long snsUserCount = snsUserRepository.count();
			log.debug(">> SNS 사용자 수: {}", snsUserCount);

			// 전체 사용자 수 계산
			long totalUserCount = regularUserCount + snsUserCount;
			log.debug(">> 전체 사용자 수: {}", totalUserCount);

			// 이벤트 수 조회
			long eventCount = eventRepository.count();
			log.debug(">> 이벤트 수: {}", eventCount);

			// 판매된 티켓 수 조회
			long soldTicketsCount = ticketRepository.count();
			log.debug(">> 판매된 티켓 수: {}", soldTicketsCount);

			// 응답 생성
			DashboardStatsResponse response = DashboardStatsResponse.builder()
				.totalUsers(totalUserCount)
				.totalEvents(eventCount)
				.soldTickets(soldTicketsCount)
				.build();

			log.debug(">> 대시보드 통계 정보 조회 완료: 사용자={}, 이벤트={}, 판매된 티켓={}",
				totalUserCount, eventCount, soldTicketsCount);

			return response;
		} catch (Exception e) {
			log.error(">> 대시보드 통계 정보 조회 중 오류: {}", e.getMessage(), e);
			throw new RuntimeException("대시보드 통계 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	/**
	 * 모든 사용자 목록을 조회합니다.
	 * 일반 사용자와 SNS 사용자를 모두 포함합니다.
	 *
	 * @return 사용자 정보가 담긴 맵
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> getAllUsers() {
		log.debug(">> 모든 사용자 목록 조회");

		Map<String, Object> result = new HashMap<>();

		try {
			// 일반 사용자 목록 조회
			List<User> regularUsers = userRepository.findAll();
			log.debug(">> 일반 사용자 목록 조회 완료: {} 명", regularUsers.size());

			// SNS 사용자 목록 조회
			List<SnsUser> snsUsers = snsUserRepository.findAll();
			log.debug(">> SNS 사용자 목록 조회 완료: {} 명", snsUsers.size());

			// 결과 맵에 저장
			result.put("regularUsers", regularUsers);
			result.put("snsUsers", snsUsers);

			log.debug(">> 모든 사용자 목록 조회 완료: 일반 사용자={}, SNS 사용자={}",
				regularUsers.size(), snsUsers.size());

			return result;
		} catch (Exception e) {
			log.error(">> 사용자 목록 조회 중 오류: {}", e.getMessage(), e);
			throw new RuntimeException("사용자 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	/**
	 * 사용자의 역할을 변경합니다.
	 * 일반 사용자와 SNS 사용자 모두 지원합니다.
	 *
	 * @param userType 사용자 타입 ("regular" 또는 "sns")
	 * @param userId 변경할 사용자의 ID
	 * @param role 변경할 역할 문자열 (USER, ADMIN, MANAGER)
	 * @return 변경된 역할 정보
	 * @throws IllegalArgumentException 사용자를 찾을 수 없거나 역할이 유효하지 않은 경우
	 */
	@Transactional
	public ModifyRoleResponse modifyRole(String userType, Long userId, String role) {
		log.debug(">> 사용자 역할 변경 시작: userType={}, userId={}, newRole={}", userType, userId, role);

		// 프론트에서 USER, ADMIN, MANAGER로 오면 ROLE_ 접두사 붙이기
		if (!role.startsWith("ROLE_")) {
			role = "ROLE_" + role;
		}
		// 역할 유효성 검사
		try {
			org.codenbug.user.domain.user.constant.UserRole.valueOf(role.replace("ROLE_", ""));
		} catch (IllegalArgumentException e) {
			log.error(">> 유효하지 않은 역할: {}", role);
			throw new IllegalArgumentException("유효하지 않은 역할입니다. USER, ADMIN, MANAGER 중 하나여야 합니다.");
		}

		// 일반 사용자인 경우
		if ("regular".equals(userType)) {
			User user = userRepository.findById(userId)
				.orElseThrow(() -> {
					log.error(">> 일반 사용자를 찾을 수 없음: userId={}", userId);
					return new IllegalArgumentException("해당 ID의 일반 사용자를 찾을 수 없습니다: " + userId);
				});

			log.debug(">> 일반 사용자 역할 변경: userId={}, 이전 역할={}, 새 역할={}",
				userId, user.getRole(), role);

			// 역할 변경
			user.updateRole(role);
			userRepository.save(user);

			log.debug(">> 일반 사용자 역할 변경 완료: userId={}, newRole={}", userId, role);
			return ModifyRoleResponse.of(role);
		}
		// SNS 사용자인 경우
		else if ("sns".equals(userType)) {
			SnsUser snsUser = snsUserRepository.findById(userId)
				.orElseThrow(() -> {
					log.error(">> SNS 사용자를 찾을 수 없음: userId={}", userId);
					return new IllegalArgumentException("해당 ID의 SNS 사용자를 찾을 수 없습니다: " + userId);
				});

			log.debug(">> SNS 사용자 역할 변경: userId={}, 이전 역할={}, 새 역할={}",
				userId, snsUser.getRole(), role);

			// 역할 변경
			snsUser.setRole(role);
			snsUserRepository.save(snsUser);

			log.debug(">> SNS 사용자 역할 변경 완료: userId={}, newRole={}", userId, role);
			return ModifyRoleResponse.of(role);
		}

		// 사용자 타입이 유효하지 않은 경우
		log.error(">> 유효하지 않은 사용자 타입: {}", userType);
		throw new IllegalArgumentException("유효하지 않은 사용자 타입입니다: " + userType);
	}

	/**
	 * 모든 이벤트 목록을 조회하고 각 이벤트의 티켓 정보를 포함하여 반환합니다.
	 *
	 * @return 이벤트 관리자 DTO 목록
	 */
	@Transactional(readOnly = true)
	public List<EventAdminDto> getAllEvents() {
		log.debug(">> 모든 이벤트 목록 조회");

		try {
			// 삭제되지 않은 이벤트만 조회
			List<Event> events = eventRepository.findAllByIsDeletedFalse();
			log.debug(">> 이벤트 목록 조회 완료: {} 개", events.size());

			// 각 이벤트에 대한 정보와 티켓 정보를 포함한 DTO 생성
			List<EventAdminDto> eventDtos = events.stream()
				.map(event -> {
					// 해당 이벤트의 판매된 티켓 수 조회
					int soldTickets = ticketRepository.countPaidTicketsByEventId(event.getEventId());
					log.debug(">> 이벤트 ID={}, 판매된 티켓 수={}", event.getEventId(), soldTickets);

					// DTO 생성
					return EventAdminDto.fromEntity(event, soldTickets);
				})
				.collect(Collectors.toList());

			log.debug(">> 이벤트 목록 조회 완료: {} 개의 이벤트", eventDtos.size());

			return eventDtos;
		} catch (Exception e) {
			log.error(">> 이벤트 목록 조회 중 오류: {}", e.getMessage(), e);
			throw new RuntimeException("이벤트 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	/**
	 * 모든 티켓 목록을 조회합니다.
	 *
	 * @return 티켓 관리자 DTO 목록
	 */
	@Transactional(readOnly = true)
	public List<TicketAdminDto> getAllTickets() {
		log.debug(">> 모든 티켓 목록 조회");

		try {
			// 모든 티켓 조회
			List<TicketAdminDto> tickets = ticketRepository.findAllTicketsForAdmin();
			log.debug(">> 티켓 목록 조회 완료: {} 개", tickets.size());

			log.debug(">> 티켓 목록 조회 완료: {} 개의 티켓", tickets.size());

			return tickets;
		} catch (Exception e) {
			log.error(">> 티켓 목록 조회 중 오류: {}", e.getMessage(), e);
			throw new RuntimeException("티켓 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	/**
	 * 특정 이벤트 정보를 조회합니다.
	 *
	 * @param eventId 조회할 이벤트 ID
	 * @return 이벤트 관리자 DTO
	 * @throws RuntimeException 이벤트를 찾을 수 없거나 오류 발생 시
	 */
	@Transactional(readOnly = true)
	public EventAdminDto getEvent(Long eventId) {
		log.debug(">> 이벤트 상세 정보 조회: id={}", eventId);

		try {
			// 이벤트 조회
			Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new RuntimeException("해당 이벤트를 찾을 수 없습니다: " + eventId));

			// 해당 이벤트의 판매된 티켓 수 조회
			int soldTickets = ticketRepository.countPaidTicketsByEventId(eventId);
			log.debug(">> 이벤트 ID={}, 판매된 티켓 수={}", event.getEventId(), soldTickets);

			// DTO 생성
			EventAdminDto eventDto = EventAdminDto.fromEntity(event, soldTickets);

			log.debug(">> 이벤트 상세 정보 조회 완료: id={}, 제목={}", eventId, eventDto.getTitle());

			return eventDto;
		} catch (Exception e) {
			log.error(">> 이벤트 상세 정보 조회 중 오류: {}", e.getMessage(), e);
			throw new RuntimeException("이벤트 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	/**
	 * 삭제 대기 중인 이벤트 목록을 조회합니다.
	 *
	 * @return 삭제 대기 중인 이벤트 목록
	 */
	@Transactional(readOnly = true)
	public List<EventAdminDto> getDeletedEvents() {
		log.debug(">> 삭제 대기 중인 이벤트 목록 조회");

		try {
			// 삭제된 이벤트 조회
			List<Event> events = eventRepository.findAllByIsDeletedTrue();
			log.debug(">> 삭제된 이벤트 목록 조회 완료: {} 개", events.size());

			// 각 이벤트에 대한 정보와 티켓 정보를 포함한 DTO 생성
			List<EventAdminDto> eventDtos = events.stream()
				.map(event -> {
					// 해당 이벤트의 판매된 티켓 수 조회
					int soldTickets = ticketRepository.countPaidTicketsByEventId(event.getEventId());
					log.debug(">> 이벤트 ID={}, 판매된 티켓 수={}", event.getEventId(), soldTickets);

					// DTO 생성
					return EventAdminDto.fromEntity(event, soldTickets);
				})
				.collect(Collectors.toList());

			log.debug(">> 삭제된 이벤트 목록 조회 완료: {} 개의 이벤트", eventDtos.size());

			return eventDtos;
		} catch (Exception e) {
			log.error(">> 삭제된 이벤트 목록 조회 중 오류: {}", e.getMessage(), e);
			throw new RuntimeException("삭제된 이벤트 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	/**
	 * 삭제된 이벤트를 복구합니다.
	 *
	 * @param eventId 복구할 이벤트 ID
	 * @param status 복구 후 설정할 이벤트 상태
	 * @return 복구된 이벤트 정보
	 * @throws IllegalAccessException 이벤트가 삭제되지 않은 경우
	 */
	@Transactional
	public EventAdminDto restoreEvent(Long eventId, EventStatusEnum status) throws IllegalAccessException {
		log.debug(">> 이벤트 복구 처리 시작: eventId={}, status={}", eventId, status);

		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> {
				log.error(">> 이벤트 복구 실패: 존재하지 않는 이벤트 - eventId={}", eventId);
				return new BadRequestException("존재하지 않는 이벤트입니다.");
			});

		// 이벤트가 삭제되지 않았다면 400에러 전송
		if (!event.getIsDeleted()) {
			log.error(">> 이벤트 복구 실패: 삭제되지 않은 이벤트 - eventId={}", eventId);
			throw new IllegalAccessException("삭제되지 않은 이벤트입니다.");
		}

		// 이벤트 상태 변경
		event.setIsDeleted(false);
		event.setStatus(status);
		log.debug(">> 이벤트 상태 변경 완료: eventId={}, status={}", eventId, status);

		// 알림 처리는 메인 로직과 분리하여 예외 처리
		try {
			// 해당 이벤트 구매자들 조회
			List<Purchase> purchases = purchaseRepository.findAllByEventId(eventId);
			log.debug(">> 이벤트 구매자 조회 완료: eventId={}, 구매자 수={}", eventId, purchases.size());

			// 모든 구매자에게 행사 복구 알림 전송
			String notificationContent = String.format(
				"[%s] 행사가 복구되었습니다. 예매 내역을 확인해주세요.",
				event.getInformation().getTitle()
			);

			for (Purchase purchase : purchases) {
				try {
					Long userId = purchase.getUser().getUserId();
					notificationService.createNotification(
						userId,
						NotificationEnum.EVENT,
						notificationContent
					);
					log.debug(">> 알림 전송 완료: userId={}, eventId={}", userId, eventId);
				} catch (Exception e) {
					log.error(">> 행사 복구 알림 전송 실패. 사용자ID: {}, 구매ID: {}, 오류: {}",
						purchase.getUser().getUserId(), purchase.getId(), e.getMessage(), e);
					// 개별 사용자 알림 실패는 다른 사용자 알림이나 이벤트 복구에 영향을 주지 않음
				}
			}
		} catch (Exception e) {
			log.error(">> 행사 복구 알림 처리 실패. 이벤트ID: {}, 오류: {}", eventId, e.getMessage(), e);
			// 알림 전체 실패는 이벤트 복구에 영향을 주지 않도록 예외를 무시함
		}

		// 복구된 이벤트 정보 반환
		int soldTickets = ticketRepository.countPaidTicketsByEventId(eventId);
		EventAdminDto eventDto = EventAdminDto.fromEntity(event, soldTickets);

		log.debug(">> 이벤트 복구 처리 완료: eventId={}", eventId);
		return eventDto;
	}

	/**
	 * 이벤트를 삭제 처리하는 메서드입니다.
	 * 관리자 권한으로 이벤트를 삭제하고, 관련된 구매자들에게 알림을 전송합니다.
	 *
	 * @param eventId 삭제할 이벤트 ID
	 * @throws IllegalAccessException 이미 삭제된 이벤트인 경우
	 */
	@Transactional
	public void deleteEvent(Long eventId) throws IllegalAccessException {
		log.debug(">> 이벤트 삭제 처리 시작: eventId={}", eventId);

		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> {
				log.error(">> 이벤트 삭제 실패: 존재하지 않는 이벤트 - eventId={}", eventId);
				return new BadRequestException("존재하지 않는 이벤트입니다.");
			});

		// 이벤트가 이미 삭제되었다면 400에러 전송
		if (event.getIsDeleted()) {
			log.error(">> 이벤트 삭제 실패: 이미 삭제된 이벤트 - eventId={}", eventId);
			throw new IllegalAccessException("이미 삭제된 이벤트입니다.");
		}

		// 이벤트 상태 변경
		event.setIsDeleted(true);
		event.setStatus(EventStatusEnum.CANCELLED);
		log.debug(">> 이벤트 상태 변경 완료: eventId={}, status=CANCELLED", eventId);

		// 알림 처리는 메인 로직과 분리하여 예외 처리
		try {
			// 해당 이벤트 구매자들 조회
			List<Purchase> purchases = purchaseRepository.findAllByEventId(eventId);
			log.debug(">> 이벤트 구매자 조회 완료: eventId={}, 구매자 수={}", eventId, purchases.size());

			// 모든 구매자에게 행사 취소 알림 전송
			String notificationContent = String.format(
				"[%s] 행사가 취소되었습니다. 예매 내역을 확인해주세요.",
				event.getInformation().getTitle()
			);

			for (Purchase purchase : purchases) {
				try {
					Long userId = purchase.getUser().getUserId();
					notificationService.createNotification(
						userId,
						NotificationEnum.EVENT,
						notificationContent
					);
					log.debug(">> 알림 전송 완료: userId={}, eventId={}", userId, eventId);
				} catch (Exception e) {
					log.error(">> 행사 취소 알림 전송 실패. 사용자ID: {}, 구매ID: {}, 오류: {}",
						purchase.getUser().getUserId(), purchase.getId(), e.getMessage(), e);
					// 개별 사용자 알림 실패는 다른 사용자 알림이나 이벤트 취소에 영향을 주지 않음
				}
			}
		} catch (Exception e) {
			log.error(">> 행사 취소 알림 처리 실패. 이벤트ID: {}, 오류: {}", eventId, e.getMessage(), e);
			// 알림 전체 실패는 이벤트 취소에 영향을 주지 않도록 예외를 무시함
		}

		log.debug(">> 이벤트 삭제 처리 완료: eventId={}", eventId);
	}
} 