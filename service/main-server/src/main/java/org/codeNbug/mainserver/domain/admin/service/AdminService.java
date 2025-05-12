package org.codeNbug.mainserver.domain.admin.service;

import org.codeNbug.mainserver.domain.admin.dto.request.AdminLoginRequest;
import org.codeNbug.mainserver.domain.admin.dto.request.AdminSignupRequest;
import org.codeNbug.mainserver.domain.admin.dto.response.AdminLoginResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.AdminSignupResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.DashboardStatsResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.ModifyRoleResponse;
import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

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
            
            // 응답 생성
            DashboardStatsResponse response = DashboardStatsResponse.builder()
                    .totalUsers(totalUserCount)
                    .totalEvents(eventCount)
                    .build();
            
            log.info(">> 대시보드 통계 정보 조회 완료: 사용자={}, 이벤트={}", 
                    totalUserCount, eventCount);
            
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
            
            // 결과 맵에 저장
            result.put("regularUsers", regularUsers);
            result.put("snsUsers", snsUsers);
            
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
        
        // 역할 유효성 검사
        try {
            org.codenbug.user.domain.user.constant.UserRole.valueOf(role);
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
} 