package org.codeNbug.mainserver.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeNbug.mainserver.domain.user.dto.request.LoginRequest;
import org.codeNbug.mainserver.domain.user.dto.request.SignupRequest;
import org.codeNbug.mainserver.domain.user.dto.request.UserUpdateRequest;
import org.codeNbug.mainserver.domain.user.dto.response.LoginResponse;
import org.codeNbug.mainserver.domain.user.dto.response.SignupResponse;
import org.codeNbug.mainserver.domain.user.dto.response.UserProfileResponse;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.codeNbug.mainserver.domain.user.repository.UserRepository;
import org.codeNbug.mainserver.global.exception.globalException.DuplicateEmailException;
import org.codeNbug.mainserver.global.exception.security.AuthenticationFailedException;
import org.codeNbug.mainserver.global.Redis.service.TokenService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /**
     * 회원가입 서비스
     *
     * @param request 회원가입 요청 정보
     * @return 회원가입 응답 정보
     * @throws DuplicateEmailException 이메일이 이미 존재하는 경우 발생하는 예외
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn(">> 회원가입 실패: 이메일 중복 - {}", request.getEmail());
            throw new DuplicateEmailException("이미 존재하는 이메일입니다.");
        }

        log.info(">> 회원가입 처리 시작: 이메일={}", request.getEmail());
        
        // 사용자 엔티티 생성 및 저장
        User user = request.toEntity(passwordEncoder);
        User savedUser = userRepository.save(user);
        
        log.info(">> 회원가입 완료: userId={}, email={}", savedUser.getUserId(), savedUser.getEmail());

        // 응답 반환
        return SignupResponse.fromEntity(savedUser);
    }

    /**
     * 로그인 서비스
     *
     * @param request 로그인 요청 정보
     * @return 로그인 응답 정보 (JWT 토큰 포함)
     * @throws AuthenticationFailedException 인증 실패 시 발생하는 예외
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info(">> 로그인 서비스 호출: 이메일={}", request.getEmail());
        
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn(">> 로그인 실패: 존재하지 않는 이메일 - {}", request.getEmail());
                    return new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요.");
                });

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn(">> 로그인 실패: 잘못된 비밀번호 - 이메일={}", request.getEmail());
            throw new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요.");
        }

        log.info(">> 인증 성공: 이메일={}, userId={}", user.getEmail(), user.getUserId());
        
        // 토큰 생성
        TokenService.TokenInfo tokenInfo = tokenService.generateTokens(user.getEmail());
        log.info(">> 토큰 생성 완료: 이메일={}", user.getEmail());
        log.debug(">> 생성된 액세스 토큰: {}", tokenInfo.getAccessToken());
        log.debug(">> 생성된 리프레시 토큰: {}", tokenInfo.getRefreshToken());

        // 응답 반환
        return LoginResponse.of(tokenInfo.getAccessToken(), tokenInfo.getRefreshToken());
    }

    /**
     * 로그아웃 처리
     * 토큰을 블랙리스트에 추가하고 Redis에서 RefreshToken을 삭제합니다.
     * 일반 회원가입 사용자와 SNS 로그인 사용자 모두 지원합니다.
     *
     * @param accessToken  액세스 토큰
     * @param refreshToken 리프레시 토큰
     */
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        log.info(">> 로그아웃 서비스 호출");
        log.debug(">> 액세스 토큰: {}", accessToken);
        log.debug(">> 리프레시 토큰: {}", refreshToken);
        
        if (accessToken == null || refreshToken == null) {
            log.warn(">> 로그아웃 실패: 토큰 값이 null - accessToken={}, refreshToken={}", 
                    accessToken != null ? "있음" : "없음", 
                    refreshToken != null ? "있음" : "없음");
            throw new AuthenticationFailedException("인증 정보가 필요합니다.");
        }

        try {
            // RefreshToken에서 subject(식별자) 추출
            // 일반 사용자 = 이메일(예: user@example.com)
            // SNS 사용자 = socialId:provider(예: 12345:GOOGLE)
            String identifier = tokenService.getSubjectFromToken(refreshToken);
            log.info(">> RefreshToken에서 식별자 추출 성공: {}", identifier);
            
            // RefreshToken 삭제
            tokenService.deleteRefreshToken(identifier);
            log.info(">> RefreshToken 삭제 완료: {}", identifier);
    
            // AccessToken 블랙리스트 처리
            long expirationTime = tokenService.getExpirationTimeFromToken(accessToken);
            tokenService.addToBlacklist(accessToken, expirationTime);
            log.info(">> AccessToken 블랙리스트 처리 완료: 만료시간={}", expirationTime);
            
            log.info(">> 로그아웃 처리 완료: 사용자={}", identifier);
        } catch (Exception e) {
            log.error(">> 로그아웃 처리 실패: {}", e.getMessage(), e);
            throw new AuthenticationFailedException("로그아웃 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 회원 탈퇴 처리
     * 사용자 정보를 삭제하고 토큰을 무효화합니다.
     * 일반 회원가입 사용자에 특화된 메서드입니다.
     * SNS 로그인 사용자의 경우 별도의 처리가 필요할 수 있습니다.
     *
     * @param email 사용자 이메일
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     */
    @Transactional
    public void withdrawUser(String email, String accessToken, String refreshToken) {
        if (email == null || accessToken == null || refreshToken == null) {
            throw new AuthenticationFailedException("인증 정보가 필요합니다.");
        }

        try {
            // RefreshToken에서 식별자(이메일 또는 socialId:provider) 추출
            String tokenIdentifier = tokenService.getSubjectFromToken(refreshToken);
            
            // 토큰에서 추출한 식별자와 파라미터로 받은 email이 일치하는지 확인
            // SNS 사용자의 경우 email이 tokenIdentifier와 다를 수 있음
            if (!tokenIdentifier.equals(email) && !tokenIdentifier.startsWith(email)) {
                throw new AuthenticationFailedException("인증 정보가 일치하지 않습니다.");
            }
            
            // 일반 사용자인 경우만 처리
            if (!tokenIdentifier.contains(":")) {
                // 사용자 존재 여부 확인
                User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                
                // 사용자 삭제
                userRepository.delete(user);
            }
            // SNS 사용자의 경우 SnsUser 엔티티 삭제는 별도 서비스에서 처리해야 함

            // RefreshToken 삭제
            tokenService.deleteRefreshToken(tokenIdentifier);

            // AccessToken 블랙리스트 처리
            long expirationTime = tokenService.getExpirationTimeFromToken(accessToken);
            tokenService.addToBlacklist(accessToken, expirationTime);
        } catch (AuthenticationFailedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationFailedException("회원 탈퇴 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회합니다.
     *
     * @return 사용자 프로필 정보
     * @throws AuthenticationFailedException 인증된 사용자가 없는 경우 발생하는 예외
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("인증된 사용자를 찾을 수 없습니다."));
        
        return UserProfileResponse.fromEntity(user);
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보를 수정합니다.
     *
     * @param request 수정할 프로필 정보
     * @return 수정된 사용자 프로필 정보
     * @throws AuthenticationFailedException 인증된 사용자가 없는 경우 발생하는 예외
     */
    @Transactional
    public UserProfileResponse updateProfile(UserUpdateRequest request) {
        // 현재 인증된 사용자 조회
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("인증된 사용자를 찾을 수 없습니다."));

        // 사용자 정보 업데이트
        user.update(
                request.getName(),
                request.getPhoneNum(),
                request.getLocation()
        );

        // 변경사항 저장 및 응답 반환
        return UserProfileResponse.fromEntity(user);
    }
}
