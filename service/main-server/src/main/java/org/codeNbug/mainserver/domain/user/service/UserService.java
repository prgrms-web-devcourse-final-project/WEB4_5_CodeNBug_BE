package org.codeNbug.mainserver.domain.user.service;

import org.codeNbug.mainserver.domain.user.dto.request.LoginRequest;
import org.codeNbug.mainserver.domain.user.dto.request.SignupRequest;
import org.codeNbug.mainserver.domain.user.dto.request.UserUpdateRequest;
import org.codeNbug.mainserver.domain.user.dto.response.LoginResponse;
import org.codeNbug.mainserver.domain.user.dto.response.SignupResponse;
import org.codeNbug.mainserver.domain.user.dto.response.UserProfileResponse;
import org.codeNbug.mainserver.global.exception.globalException.DuplicateEmailException;
import org.codeNbug.mainserver.global.util.SecurityUtil;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.redis.service.TokenService;
import org.codenbug.user.security.exception.AuthenticationFailedException;
import org.codenbug.user.sns.Entity.SnsUser;
import org.codenbug.user.sns.repository.SnsUserRepository;
import org.codenbug.user.sns.util.KakaoOauth;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;

/**
 * 사용자 관련 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final SnsUserRepository snsUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final KakaoOauth kakaoOauth;

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
            // 1. RefreshToken에서 subject(식별자) 추출
            String identifier = tokenService.getSubjectFromToken(refreshToken);
            log.info(">> RefreshToken에서 식별자 추출 성공: {}", identifier);

            // 2. AccessToken에서 subject(식별자) 추출 및 RefreshToken과 일치 여부 확인
            String accessTokenIdentifier = tokenService.getSubjectFromToken(accessToken);
            if (!identifier.equals(accessTokenIdentifier)) {
                log.warn(">> 토큰 불일치: 액세스 토큰 식별자={}, 리프레시 토큰 식별자={}", accessTokenIdentifier, identifier);
                throw new AuthenticationFailedException("액세스 토큰과 리프레시 토큰의 사용자 정보가 일치하지 않습니다.");
            }

            // 3. SNS 사용자인 경우 SNS 타입에 따라 외부 API 호출
            if (identifier != null && identifier.contains(":")) {
                handleSnsLogout(identifier, accessToken);
            }

            // 4. RefreshToken 삭제
            tokenService.deleteRefreshToken(identifier);
            log.info(">> RefreshToken 삭제 완료: {}", identifier);

            // 5. AccessToken 블랙리스트 처리
            long expirationTime = tokenService.getExpirationTimeFromToken(accessToken);
            tokenService.addToBlacklist(accessToken, expirationTime);
            log.info(">> AccessToken 블랙리스트 처리 완료: 만료시간={}", expirationTime);
            
            log.info(">> 로그아웃 처리 완료: 사용자={}", identifier);
        } catch (AuthenticationFailedException e) {
            log.error(">> 로그아웃 인증 실패: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error(">> 로그아웃 처리 실패: {}", e.getMessage(), e);
            throw new AuthenticationFailedException("로그아웃 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * SNS 사용자 로그아웃 처리
     * 소셜 로그인 타입에 따라 적절한 로그아웃 API를 호출합니다.
     *
     * @param identifier SNS 사용자 식별자 (socialId:provider 형식)
     * @param accessToken 액세스 토큰
     */
    private void handleSnsLogout(String identifier, String accessToken) {
        try {
            String[] parts = identifier.split(":");
            if (parts.length != 2) {
                log.warn(">> 잘못된 SNS 사용자 식별자 형식: {}", identifier);
                return;
            }

            String socialId = parts[0];
            String provider = parts[1];
            
            log.info(">> SNS 로그아웃 처리: socialId={}, provider={}", socialId, provider);

            // SNS 제공자별 로그아웃 처리
            switch (provider) {
                case "KAKAO":
                    log.info(">> 카카오 로그아웃 API 호출");
                    kakaoOauth.kakaoLogout(accessToken);
                    break;
                case "GOOGLE":
                    log.info(">> 구글 로그아웃 처리 (클라이언트 측에서 처리됨)");
                    // 구글은 서버 측에서 로그아웃 API를 호출할 필요가 없거나 별도 처리가 필요
                    break;
                case "NAVER":
                    log.info(">> 네이버 로그아웃 처리 (향후 구현 예정)");
                    // 네이버 로그아웃 처리 로직 추가 (필요시)
                    break;
                default:
                    log.warn(">> 지원되지 않는 SNS 제공자: {}", provider);
            }
        } catch (Exception e) {
            log.error(">> SNS 로그아웃 처리 중 오류 발생: {}", e.getMessage(), e);
            // 로그아웃 전체 프로세스를 중단하지 않도록 예외를 던지지 않고 로깅만 함
        }
    }

    /**
     * 회원 탈퇴 처리
     * 사용자 정보를 삭제하고 토큰을 무효화합니다.
     * 일반 사용자와 SNS 사용자 모두 지원합니다.
     *
     * @param identifier 사용자 식별자 (이메일 또는 socialId:provider)
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     */
    @Transactional
    public void withdrawUser(String identifier, String accessToken, String refreshToken) {
        log.info(">> 회원 탈퇴 처리 시작: identifier={}", identifier);
        
        if (identifier == null || accessToken == null || refreshToken == null) {
            log.warn(">> 회원 탈퇴 실패: 인증 정보 누락");
            throw new AuthenticationFailedException("인증 정보가 필요합니다.");
        }

        try {
            // RefreshToken에서 식별자(이메일 또는 socialId:provider) 추출
            String tokenIdentifier = tokenService.getSubjectFromToken(refreshToken);
            log.debug(">> 토큰에서 추출한 식별자: {}", tokenIdentifier);
            
            // 토큰에서 추출한 식별자와 파라미터로 받은 identifier가 일치하는지 확인
            if (!tokenIdentifier.equals(identifier)) {
                log.warn(">> 회원 탈퇴 실패: 인증 정보 불일치 - token={}, param={}", tokenIdentifier, identifier);
                throw new AuthenticationFailedException("인증 정보가 일치하지 않습니다.");
            }
            
            // SNS 사용자인 경우 (identifier에 : 포함)
            if (tokenIdentifier.contains(":")) {
                log.info(">> SNS 사용자 탈퇴 처리: {}", tokenIdentifier);
                String[] parts = tokenIdentifier.split(":");
                if (parts.length != 2) {
                    log.warn(">> 잘못된 SNS 사용자 식별자 형식: {}", tokenIdentifier);
                    throw new IllegalArgumentException("잘못된 SNS 사용자 식별자 형식입니다.");
                }
                
                String socialId = parts[0];
                String provider = parts[1];
                
                // SNS 계정 연동 해제 처리
                handleSnsWithdraw(socialId, provider, accessToken);
                
                // SnsUser 엔티티 삭제
                SnsUser snsUser = snsUserRepository.findBySocialId(socialId)
                    .orElseThrow(() -> {
                        log.warn(">> 탈퇴할 SNS 사용자를 찾을 수 없음: socialId={}", socialId);
                        return new IllegalArgumentException("SNS 사용자를 찾을 수 없습니다.");
                    });
                
                log.info(">> SNS 사용자 엔티티 삭제: id={}, socialId={}, provider={}", 
                        snsUser.getId(), snsUser.getSocialId(), snsUser.getProvider());
                snsUserRepository.delete(snsUser);
            }
            // 일반 사용자인 경우
            else {
                log.info(">> 일반 사용자 탈퇴 처리: {}", tokenIdentifier);
                
                // 사용자 존재 여부 확인
                User user = userRepository.findByEmail(tokenIdentifier)
                    .orElseThrow(() -> {
                        log.warn(">> 탈퇴할 일반 사용자를 찾을 수 없음: email={}", tokenIdentifier);
                        return new IllegalArgumentException("사용자를 찾을 수 없습니다.");
                    });
                
                log.info(">> 일반 사용자 엔티티 삭제: userId={}, email={}", user.getUserId(), user.getEmail());
                // 사용자 삭제
                userRepository.delete(user);
            }

            // RefreshToken 삭제
            tokenService.deleteRefreshToken(tokenIdentifier);
            log.info(">> RefreshToken 삭제 완료: {}", tokenIdentifier);

            // AccessToken 블랙리스트 처리
            long expirationTime = tokenService.getExpirationTimeFromToken(accessToken);
            tokenService.addToBlacklist(accessToken, expirationTime);
            log.info(">> AccessToken 블랙리스트 처리 완료: 만료시간={}", expirationTime);
            
            log.info(">> 회원 탈퇴 처리 완료: identifier={}", tokenIdentifier);
        } catch (AuthenticationFailedException | IllegalArgumentException e) {
            log.error(">> 회원 탈퇴 처리 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error(">> 회원 탈퇴 처리 중 오류: {}", e.getMessage(), e);
            throw new AuthenticationFailedException("회원 탈퇴 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * SNS 사용자 탈퇴 시 SNS 계정 연동 해제 처리
     * 
     * @param socialId SNS 사용자 ID
     * @param provider SNS 제공자 (KAKAO, GOOGLE, NAVER 등)
     * @param accessToken 액세스 토큰
     */
    private void handleSnsWithdraw(String socialId, String provider, String accessToken) {
        log.info(">> SNS 계정 연동 해제 처리: socialId={}, provider={}", socialId, provider);
        
        try {
            switch (provider) {
                case "KAKAO":
                    log.info(">> 카카오 연동 해제 API 호출");
                    // 카카오 계정 연동 해제 호출
                    kakaoOauth.kakaoUnlink(accessToken);
                    break;
                case "GOOGLE":
                    log.info(">> 구글 연동 해제 처리 (클라이언트 측에서 주로 처리)");
                    // 구글은 서버 측에서 연동 해제 호출이 필요 없거나 별도 처리 필요
                    // 필요시 구글 API 호출 로직 구현
                    break;
                case "NAVER":
                    log.info(">> 네이버 연동 해제 처리 (향후 구현 예정)");
                    // 네이버 연동 해제 처리 로직 (필요시)
                    break;
                default:
                    log.warn(">> 지원되지 않는 SNS 제공자: {}", provider);
            }
        } catch (Exception e) {
            // 연동 해제 실패해도 사용자 탈퇴는 진행
            log.error(">> SNS 계정 연동 해제 처리 중 오류: {}", e.getMessage(), e);
            log.info(">> SNS 계정 연동 해제 실패했으나, 사용자 탈퇴는 계속 진행됩니다.");
        }
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회합니다.
     * 일반 사용자와 SNS 사용자 모두 지원합니다.
     *
     * @return 사용자 프로필 정보
     * @throws AuthenticationFailedException 인증된 사용자가 없는 경우 발생하는 예외
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        log.debug(">> 사용자 프로필 조회 시작");
        
        try {
            // 일반 사용자인 경우
            if (SecurityUtil.isRegularUser()) {
                log.debug(">> 일반 사용자 프로필 조회");
                User user = SecurityUtil.getCurrentUser();
                return UserProfileResponse.fromEntity(user);
            }
            // SNS 사용자인 경우
            else {
                log.debug(">> SNS 사용자 프로필 조회");
                SnsUser snsUser = SecurityUtil.getCurrentSnsUser();
                return UserProfileResponse.fromSnsEntity(snsUser);
            }
        } catch (Exception e) {
            log.error(">> 프로필 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new AuthenticationFailedException("프로필 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보를 수정합니다.
     * 일반 사용자와 SNS 사용자 모두 지원합니다.
     *
     * @param request 수정할 프로필 정보
     * @return 수정된 사용자 프로필 정보
     * @throws AuthenticationFailedException 인증된 사용자가 없는 경우 발생하는 예외
     */
    @Transactional
    public UserProfileResponse updateProfile(UserUpdateRequest request) {
        log.debug(">> 사용자 프로필 수정 시작: name={}, phoneNum={}, location={}", 
                 request.getName(), request.getPhoneNum(), request.getLocation());
        
        try {
            // 일반 사용자인 경우
            if (SecurityUtil.isRegularUser()) {
                log.debug(">> 일반 사용자 프로필 수정");
                User user = SecurityUtil.getCurrentUser();
                
                // 사용자 정보 업데이트
                user.update(
                        request.getName(),
                        request.getPhoneNum(),
                        request.getLocation()
                );
                
                // 변경사항 저장 및 응답 반환
                return UserProfileResponse.fromEntity(user);
            }
            // SNS 사용자인 경우
            else {
                log.debug(">> SNS 사용자 프로필 수정");
                SnsUser snsUser = SecurityUtil.getCurrentSnsUser();
                
                // SNS 사용자 정보 업데이트
                snsUser.update(
                        request.getName(),
                        request.getPhoneNum(),
                        request.getLocation()
                );
                
                // 변경사항 저장 및 응답 반환
                snsUserRepository.save(snsUser);
                return UserProfileResponse.fromSnsEntity(snsUser);
            }
        } catch (Exception e) {
            log.error(">> 프로필 수정 중 오류 발생: {}", e.getMessage(), e);
            throw new AuthenticationFailedException("프로필 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
