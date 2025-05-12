package org.codeNbug.mainserver.domain.admin.service;

import org.codeNbug.mainserver.domain.admin.dto.request.AdminLoginRequest;
import org.codeNbug.mainserver.domain.admin.dto.request.AdminSignupRequest;
import org.codeNbug.mainserver.domain.admin.dto.response.AdminLoginResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.AdminSignupResponse;
import org.codeNbug.mainserver.global.exception.globalException.DuplicateEmailException;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.redis.service.TokenService;
import org.codenbug.user.security.exception.AuthenticationFailedException;
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
} 