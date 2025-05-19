package org.codeNbug.mainserver.domain.admin.service;

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
import org.codeNbug.mainserver.domain.user.service.LoginAttemptService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

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
    private final LoginAttemptService loginAttemptService;

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
            log.warn(">> 관리자 회원가입 실패: 이메일 중복 - {}", request.getEmail());
            throw new DuplicateEmailException("이미 존재하는 이메일입니다.");
        }

        try {
            log.info(">> 관리자 회원가입 처리 시작: 이메일={}", request.getEmail());
            
            // 사용자 엔티티 생성 및 저장 (ROLE_ADMIN으로 설정)
            User admin = request.toEntity(passwordEncoder);
            log.debug(">> User 엔티티 생성 완료: {}", admin);
            
            User savedAdmin = userRepository.save(admin);
            log.debug(">> User 엔티티 저장 완료: id={}", savedAdmin.getUserId());
            
            log.info(">> 관리자 회원가입 완료: userId={}, email={}", savedAdmin.getUserId(), savedAdmin.getEmail());

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
        log.info(">> 관리자 로그인 서비스 호출: 이메일={}", request.getEmail());
        
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn(">> 관리자 로그인 실패: 존재하지 않는 이메일 - {}", request.getEmail());
                    return new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요.");
                });

        // 관리자 권한 확인
        if (!"ROLE_ADMIN".equals(user.getRole())) {
            log.warn(">> 관리자 로그인 실패: 관리자 권한 없음 - 이메일={}, 현재 권한={}", request.getEmail(), user.getRole());
            throw new AuthenticationFailedException("관리자 권한이 없습니다.");
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn(">> 관리자 로그인 실패: 잘못된 비밀번호 - 이메일={}", request.getEmail());
            throw new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요.");
        }

        log.info(">> 관리자 인증 성공: 이메일={}, userId={}", user.getEmail(), user.getUserId());
        
        // 토큰 생성
        TokenService.TokenInfo tokenInfo = tokenService.generateTokens(user.getEmail());
        log.info(">> 토큰 생성 완료: 이메일={}", user.getEmail());

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
        log.info(">> 관리자 로그아웃 서비스 호출");
        
        if (accessToken == null || refreshToken == null) {
            log.warn(">> 관리자 로그아웃 실패: 토큰 값이 null");
            throw new AuthenticationFailedException("인증 정보가 필요합니다.");
        }

        try {
            // RefreshToken에서 subject(식별자) 추출
            String identifier = tokenService.getSubjectFromToken(refreshToken);
            log.info(">> RefreshToken에서 식별자 추출 성공: {}", identifier);

            // AccessToken에서 subject(식별자) 추출 및 RefreshToken과 일치 여부 확인
            String accessTokenIdentifier = tokenService.getSubjectFromToken(accessToken);
            if (!identifier.equals(accessTokenIdentifier)) {
                log.warn(">> 토큰 불일치: 액세스 토큰 식별자={}, 리프레시 토큰 식별자={}", accessTokenIdentifier, identifier);
                throw new AuthenticationFailedException("액세스 토큰과 리프레시 토큰의 사용자 정보가 일치하지 않습니다.");
            }

            // RefreshToken 삭제
            tokenService.deleteRefreshToken(identifier);
            log.info(">> RefreshToken 삭제 완료: {}", identifier);

            // AccessToken 블랙리스트 처리
            long expirationTime = tokenService.getExpirationTimeFromToken(accessToken);
            tokenService.addToBlacklist(accessToken, expirationTime);
            log.info(">> AccessToken 블랙리스트 처리 완료: 만료시간={}", expirationTime);
            
            log.info(">> 관리자 로그아웃 처리 완료: 사용자={}", identifier);
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
        log.info(">> 대시보드 통계 정보 조회");
        
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
            
            log.info(">> 대시보드 통계 정보 조회 완료: 사용자={}, 이벤트={}, 판매된 티켓={}", 
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
        log.info(">> 모든 사용자 목록 조회");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 일반 사용자 목록 조회
            List<User> regularUsers = userRepository.findAll();
            log.debug(">> 일반 사용자 목록 조회 완료: {} 명", regularUsers.size());
            
            // SNS 사용자 목록 조회
            List<SnsUser> snsUsers = snsUserRepository.findAll();
            log.debug(">> SNS 사용자 목록 조회 완료: {} 명", snsUsers.size());
            
            // Redis의 계정 잠금 상태 확인 및 Map 생성
            Map<String, Boolean> redisLockStatus = new HashMap<>();
            Map<String, Long> redisLockRemainingTime = new HashMap<>();
            
            // 일반 사용자의 Redis 계정 잠금 상태 확인
            for (User user : regularUsers) {
                String email = user.getEmail();
                boolean isLockedInRedis = loginAttemptService.isAccountLocked(email);
                redisLockStatus.put(email, isLockedInRedis);
                
                if (isLockedInRedis) {
                    long remainingTime = loginAttemptService.getRemainingLockTimeByEmail(email);
                    redisLockRemainingTime.put(email, remainingTime);
                }
            }
            
            // 결과 맵에 저장
            result.put("regularUsers", regularUsers);
            result.put("snsUsers", snsUsers);
            result.put("redisLockStatus", redisLockStatus);
            result.put("redisLockRemainingTime", redisLockRemainingTime);
            
            log.info(">> 모든 사용자 목록 조회 완료: 일반 사용자={}, SNS 사용자={}", 
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
        log.info(">> 사용자 역할 변경 시작: userType={}, userId={}, newRole={}", userType, userId, role);
        
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
            
            log.info(">> 일반 사용자 역할 변경: userId={}, 이전 역할={}, 새 역할={}", 
                   userId, user.getRole(), role);
            
            // 역할 변경
            user.updateRole(role);
            userRepository.save(user);
            
            log.info(">> 일반 사용자 역할 변경 완료: userId={}, newRole={}", userId, role);
            return ModifyRoleResponse.of(role);
        }
        // SNS 사용자인 경우
        else if ("sns".equals(userType)) {
            SnsUser snsUser = snsUserRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error(">> SNS 사용자를 찾을 수 없음: userId={}", userId);
                        return new IllegalArgumentException("해당 ID의 SNS 사용자를 찾을 수 없습니다: " + userId);
                    });
            
            log.info(">> SNS 사용자 역할 변경: userId={}, 이전 역할={}, 새 역할={}", 
                   userId, snsUser.getRole(), role);
            
            // 역할 변경
            snsUser.setRole(role);
            snsUserRepository.save(snsUser);
            
            log.info(">> SNS 사용자 역할 변경 완료: userId={}, newRole={}", userId, role);
            return ModifyRoleResponse.of(role);
        }
        
        // 사용자 타입이 유효하지 않은 경우
        log.error(">> 유효하지 않은 사용자 타입: {}", userType);
        throw new IllegalArgumentException("유효하지 않은 사용자 타입입니다: " + userType);
    }

    /**
     * 모든 이벤트 목록을 조회합니다.
     * 배치 작업에 의해 자동으로 업데이트된 상태 값을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<EventAdminDto> getAllEvents() {
        log.info(">> 모든 이벤트 목록 조회");
        
        try {
            // 삭제되지 않은 이벤트 조회
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
            
            log.info(">> 이벤트 목록 조회 완료: {} 개의 이벤트", eventDtos.size());
            
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
        log.info(">> 모든 티켓 목록 조회");
        
        try {
            // 모든 티켓 조회
            List<TicketAdminDto> tickets = ticketRepository.findAllTicketsForAdmin();
            log.debug(">> 티켓 목록 조회 완료: {} 개", tickets.size());
            
            log.info(">> 티켓 목록 조회 완료: {} 개의 티켓", tickets.size());
            
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
        log.info(">> 이벤트 상세 정보 조회: id={}", eventId);
        
        try {
            // 이벤트 조회
            Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("해당 이벤트를 찾을 수 없습니다: " + eventId));
            
            // 해당 이벤트의 판매된 티켓 수 조회
            int soldTickets = ticketRepository.countPaidTicketsByEventId(eventId);
            log.debug(">> 이벤트 ID={}, 판매된 티켓 수={}", event.getEventId(), soldTickets);
            
            // DTO 생성
            EventAdminDto eventDto = EventAdminDto.fromEntity(event, soldTickets);
            
            log.info(">> 이벤트 상세 정보 조회 완료: id={}, 제목={}", eventId, eventDto.getTitle());
            
            return eventDto;
        } catch (Exception e) {
            log.error(">> 이벤트 상세 정보 조회 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("이벤트 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 삭제된 이벤트 목록을 조회합니다.
     * 삭제된 이벤트는 CANCELLED 상태를 유지합니다.
     */
    @Transactional(readOnly = true)
    public List<EventAdminDto> getDeletedEvents() {
        log.info(">> 삭제 대기 중인 이벤트 목록 조회");
        
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
            
            log.info(">> 삭제된 이벤트 목록 조회 완료: {} 개의 이벤트", eventDtos.size());
            
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
     * @return 복구된 이벤트 정보
     * @throws IllegalAccessException 이벤트가 삭제되지 않은 경우
     */
    @Transactional
    public EventAdminDto restoreEvent(Long eventId) throws IllegalAccessException {
        log.info(">> 이벤트 복구 처리 시작: eventId={}", eventId);
        
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error(">> 이벤트 복구 실패: 존재하지 않는 이벤트 - eventId={}", eventId);
                    return new BadRequestException("존재하지 않는 이벤트입니다.");
                });

        // 이벤트가 삭제되지 않았다면 400에러 전송
        if (!event.getIsDeleted()) {
            log.warn(">> 이벤트 복구 실패: 삭제되지 않은 이벤트 - eventId={}", eventId);
            throw new IllegalAccessException("삭제되지 않은 이벤트입니다.");
        }

        // 이벤트 상태 변경
        event.setIsDeleted(false);
        
        // 예매 시작일과 종료일을 기준으로 상태 설정
        LocalDateTime now = LocalDateTime.now();
        if (event.getBookingStart() != null && event.getBookingEnd() != null) {
            if (!now.isBefore(event.getBookingStart()) && !now.isAfter(event.getBookingEnd())) {
                event.setStatus(EventStatusEnum.OPEN); // 예매 중
            } else {
                event.setStatus(EventStatusEnum.CLOSED); // 예매 종료
            }
        } else {
            event.setStatus(EventStatusEnum.CLOSED); // 날짜 미입력 → CLOSED
        }
        
        log.info(">> 이벤트 상태 변경 완료: eventId={}, status={}", eventId, event.getStatus());

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
        
        log.info(">> 이벤트 복구 처리 완료: eventId={}", eventId);
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
        log.info(">> 이벤트 삭제 처리 시작: eventId={}", eventId);
        
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error(">> 이벤트 삭제 실패: 존재하지 않는 이벤트 - eventId={}", eventId);
                    return new BadRequestException("존재하지 않는 이벤트입니다.");
                });

        // 이벤트가 이미 삭제되었다면 400에러 전송
        if (event.getIsDeleted()) {
            log.warn(">> 이벤트 삭제 실패: 이미 삭제된 이벤트 - eventId={}", eventId);
            throw new IllegalAccessException("이미 삭제된 이벤트입니다.");
        }

        // 이벤트 상태 변경
        event.setIsDeleted(true);
        event.setStatus(EventStatusEnum.CANCELLED);
        log.info(">> 이벤트 상태 변경 완료: eventId={}, status=CANCELLED", eventId);

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

        log.info(">> 이벤트 삭제 처리 완료: eventId={}", eventId);
    }

    /**
     * 계정 만료일을 연장합니다.
     *
     * @param userId 사용자 ID
     */
    public void extendAccountExpiry(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setAccountExpiredAt(LocalDateTime.now().plusMonths(6));
        userRepository.save(user);
    }

    /**
     * 비밀번호 만료일을 연장합니다.
     *
     * @param userId 사용자 ID
     */
    public void extendPasswordExpiry(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setPasswordExpiredAt(LocalDateTime.now().plusMonths(3));
        userRepository.save(user);
    }

    /**
     * 계정을 비활성화합니다.
     *
     * @param userId 사용자 ID
     */
    public void disableAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setEnabled(false);
        userRepository.save(user);
    }

    /**
     * 계정을 활성화합니다.
     *
     * @param userId 사용자 ID
     */
    public void enableAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setEnabled(true);
        userRepository.save(user);
    }

    /**
     * 계정 잠금을 해제합니다.
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void unlockAccount(Long userId) {
        log.info(">> 관리자에 의한 계정 잠금 해제: userId={}", userId);
        
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(">> 계정 잠금 해제 실패: 사용자를 찾을 수 없음 - userId={}", userId);
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다.");
                });
        
        // 사용자 이메일 가져오기
        String email = user.getEmail();
        
        // 기존 잠금 상태 확인
        boolean isLockedInRedis = loginAttemptService.isAccountLocked(email);
        boolean isLockedInDb = user.isAccountLocked();
        
        log.info(">> 계정 잠금 상태: userId={}, email={}, Redis 잠금={}, DB 잠금={}", 
                userId, email, isLockedInRedis, isLockedInDb);
        
        // 계정 잠금 해제 - 이메일 기반 메소드 사용
        boolean success = loginAttemptService.resetAttemptByEmail(email);
        
        // DB에서 잠금 상태 해제
        if (isLockedInDb) {
            user.setAccountLocked(false);
            userRepository.save(user);
            log.info(">> DB 계정 잠금 상태 업데이트됨: userId={}", userId);
        }
        
        if (success) {
            log.info(">> 계정 잠금 해제 성공: userId={}, email={}", userId, email);
        } else {
            // Redis에서 직접 계정 잠금 해제 시도 (백업 방법)
            if (isLockedInRedis) {
                try {
                    log.info(">> 백업 방법으로 Redis 계정 잠금 해제 시도: email={}", email);
                    String key = "accountLock:" + email;
                    boolean deleted = loginAttemptService.getRedisTemplate().delete(key);
                    log.info(">> Redis 직접 삭제 결과: {}", deleted);
                } catch (Exception e) {
                    log.error(">> Redis 계정 잠금 직접 해제 중 오류: {}", e.getMessage(), e);
                }
            }
            
            log.warn(">> 표준 방법으로 계정 잠금 해제 실패, 백업 방법 시도: userId={}", userId);
            
            // 여전히 잠금 상태인지 확인
            if (loginAttemptService.isAccountLocked(email)) {
                log.error(">> 계정 잠금 해제 최종 실패: userId={}, email={}", userId, email);
                throw new RuntimeException("계정 잠금 해제에 실패했습니다.");
            } else {
                log.info(">> 백업 방법으로 계정 잠금 해제 성공: userId={}, email={}", userId, email);
            }
        }
    }

    /**
     * 모든 사용자의 로그인 시도 횟수를 초기화하는 관리자용 메서드
     * 주의: 관리자 권한으로만 실행되어야 합니다.
     *
     * @return 초기화된 사용자 수
     */
    @Transactional
    public int resetAllLoginAttemptCounts() {
        log.info(">> 관리자에 의한 모든 사용자 로그인 시도 횟수 초기화 시작");
        return loginAttemptService.resetAllLoginAttempts();
    }

    /**
     * 모든 사용자의 계정 잠금 상태를 Redis와 DB 간에 동기화합니다.
     * 이 메서드는 관리자 권한으로 수동 실행 가능하며, 기본적으로 maintainUserAccounts에서 
     * 매일 새벽 2시에 자동으로 실행되는 로직과 유사합니다.
     * 
     * @return 동기화된 사용자 수
     */
    @Transactional
    public int syncUserLockStatus() {
        log.info(">> 사용자 계정 잠금 상태 동기화 시작");
        AtomicInteger syncCount = new AtomicInteger(0);
        
        try {
            // 1. DB에는 잠겨있지만 Redis에는 잠금 정보가 없는 계정 해제
            List<User> lockedUsers = userRepository.findByAccountLockedTrue();
            log.info(">> DB에서 잠겨있는 계정 수: {}", lockedUsers.size());
            
            for (User user : lockedUsers) {
                String email = user.getEmail();
                if (!loginAttemptService.isAccountLocked(email)) {
                    // Redis에는 잠금 정보가 없는 경우, DB에서도 해제
                    user.setAccountLocked(false);
                    userRepository.save(user);
                    syncCount.incrementAndGet();
                    log.info(">> DB 잠금 해제 동기화: email={}", email);
                }
            }
            
            // 2. Redis에는 잠겨있지만 DB에는 잠금 정보가 없는 계정 동기화
            // Redis에서 모든 잠금 키 조회
            Set<String> lockKeys = loginAttemptService.getRedisTemplate().keys("accountLock:*");
            
            if (lockKeys != null && !lockKeys.isEmpty()) {
                log.info(">> Redis에서 잠겨있는 계정 수: {}", lockKeys.size());
                
                for (String key : lockKeys) {
                    // 키에서 이메일 추출 (accountLock: 제거)
                    String email = key.substring("accountLock:".length());
                    
                    // 해당 이메일로 사용자 조회
                    userRepository.findByEmail(email).ifPresent(user -> {
                        if (!user.isAccountLocked()) {
                            user.setAccountLocked(true);
                            userRepository.save(user);
                            syncCount.incrementAndGet();
                            log.info(">> DB 잠금 설정 동기화: email={}", email);
                        }
                    });
                }
            }
            
            log.info(">> 계정 잠금 상태 동기화 완료: {}개 계정 동기화됨", syncCount.get());
            return syncCount.get();
        } catch (Exception e) {
            log.error(">> 계정 잠금 상태 동기화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("계정 잠금 상태 동기화 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 여러 계정을 동시에 활성화합니다.
     * 관리자 권한으로만 실행되어야 합니다.
     * 
     * @param userIds 활성화할 사용자 ID 목록
     * @return 처리된 계정 수
     */
    @Transactional
    public int bulkEnableAccounts(List<Long> userIds) {
        log.info(">> 일괄 계정 활성화 시작: {}개 계정", userIds.size());
        
        if (userIds.isEmpty()) {
            return 0;
        }
        
        AtomicInteger processedCount = new AtomicInteger(0);
        
        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId)
                        .orElse(null);
                
                if (user != null) {
                    user.setEnabled(true);
                    userRepository.save(user);
                    processedCount.incrementAndGet();
                    log.info(">> 계정 활성화 성공: userId={}", userId);
                } else {
                    log.warn(">> 계정 활성화 실패: 사용자를 찾을 수 없음 - userId={}", userId);
                }
            } catch (Exception e) {
                log.error(">> 계정 활성화 중 오류 발생: userId={}, 오류={}", userId, e.getMessage(), e);
                // 개별 실패는 전체 프로세스를 중단하지 않음
            }
        }
        
        log.info(">> 일괄 계정 활성화 완료: {}개 처리됨", processedCount.get());
        return processedCount.get();
    }
    
    /**
     * 여러 계정을 동시에 비활성화합니다.
     * 관리자 권한으로만 실행되어야 합니다.
     * 
     * @param userIds 비활성화할 사용자 ID 목록
     * @return 처리된 계정 수
     */
    @Transactional
    public int bulkDisableAccounts(List<Long> userIds) {
        log.info(">> 일괄 계정 비활성화 시작: {}개 계정", userIds.size());
        
        if (userIds.isEmpty()) {
            return 0;
        }
        
        AtomicInteger processedCount = new AtomicInteger(0);
        
        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId)
                        .orElse(null);
                
                if (user != null) {
                    user.setEnabled(false);
                    userRepository.save(user);
                    processedCount.incrementAndGet();
                    log.info(">> 계정 비활성화 성공: userId={}", userId);
                } else {
                    log.warn(">> 계정 비활성화 실패: 사용자를 찾을 수 없음 - userId={}", userId);
                }
            } catch (Exception e) {
                log.error(">> 계정 비활성화 중 오류 발생: userId={}, 오류={}", userId, e.getMessage(), e);
                // 개별 실패는 전체 프로세스를 중단하지 않음
            }
        }
        
        log.info(">> 일괄 계정 비활성화 완료: {}개 처리됨", processedCount.get());
        return processedCount.get();
    }
    
    /**
     * 여러 계정을 동시에 잠급니다.
     * 관리자 권한으로만 실행되어야 합니다.
     * 
     * @param userIds 잠글 사용자 ID 목록
     * @return 처리된 계정 수
     */
    @Transactional
    public int bulkLockAccounts(List<Long> userIds) {
        log.info(">> 일괄 계정 잠금 시작: {}개 계정", userIds.size());
        
        if (userIds.isEmpty()) {
            return 0;
        }
        
        AtomicInteger processedCount = new AtomicInteger(0);
        
        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId)
                        .orElse(null);
                
                if (user != null) {
                    // DB에서 계정 잠금 설정
                    user.setAccountLocked(true);
                    userRepository.save(user);
                    
                    // Redis에도 계정 잠금 설정
                    String email = user.getEmail();
                    if (email != null && !email.isEmpty()) {
                        String key = "accountLock:" + email;
                        loginAttemptService.getRedisTemplate().opsForValue().set(
                            key, LocalDateTime.now().toString(), 30, TimeUnit.MINUTES);
                        log.info(">> Redis에 계정 잠금 설정: email={}", email);
                    }
                    
                    processedCount.incrementAndGet();
                    log.info(">> 계정 잠금 성공: userId={}", userId);
                } else {
                    log.warn(">> 계정 잠금 실패: 사용자를 찾을 수 없음 - userId={}", userId);
                }
            } catch (Exception e) {
                log.error(">> 계정 잠금 중 오류 발생: userId={}, 오류={}", userId, e.getMessage(), e);
                // 개별 실패는 전체 프로세스를 중단하지 않음
            }
        }
        
        log.info(">> 일괄 계정 잠금 완료: {}개 처리됨", processedCount.get());
        return processedCount.get();
    }
    
    /**
     * 여러 계정을 동시에 잠금 해제합니다.
     * 관리자 권한으로만 실행되어야 합니다.
     * 
     * @param userIds 잠금 해제할 사용자 ID 목록
     * @return 처리된 계정 수
     */
    @Transactional
    public int bulkUnlockAccounts(List<Long> userIds) {
        log.info(">> 일괄 계정 잠금 해제 시작: {}개 계정", userIds.size());
        
        if (userIds.isEmpty()) {
            return 0;
        }
        
        AtomicInteger processedCount = new AtomicInteger(0);
        
        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId)
                        .orElse(null);
                
                if (user != null) {
                    // DB에서 계정 잠금 해제
                    user.setAccountLocked(false);
                    userRepository.save(user);
                    
                    // Redis에서도 계정 잠금 해제 및 로그인 시도 횟수 초기화
                    String email = user.getEmail();
                    if (email != null && !email.isEmpty()) {
                        // 계정 잠금 키 삭제
                        String lockKey = "accountLock:" + email;
                        Boolean lockDeleted = loginAttemptService.getRedisTemplate().delete(lockKey);
                        
                        // 로그인 시도 횟수 키 삭제
                        String attemptKey = "loginAttempt:" + email;
                        Boolean attemptDeleted = loginAttemptService.getRedisTemplate().delete(attemptKey);
                        
                        log.info(">> Redis에서 계정 잠금 해제: email={}, 잠금삭제={}, 시도횟수삭제={}", 
                                email, lockDeleted, attemptDeleted);
                    }
                    
                    processedCount.incrementAndGet();
                    log.info(">> 계정 잠금 해제 성공: userId={}", userId);
                } else {
                    log.warn(">> 계정 잠금 해제 실패: 사용자를 찾을 수 없음 - userId={}", userId);
                }
            } catch (Exception e) {
                log.error(">> 계정 잠금 해제 중 오류 발생: userId={}, 오류={}", userId, e.getMessage(), e);
                // 개별 실패는 전체 프로세스를 중단하지 않음
            }
        }
        
        log.info(">> 일괄 계정 잠금 해제 완료: {}개 처리됨", processedCount.get());
        return processedCount.get();
    }
} 