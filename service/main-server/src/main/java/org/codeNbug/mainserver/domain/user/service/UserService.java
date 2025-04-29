package org.codeNbug.mainserver.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.user.dto.request.LoginRequest;
import org.codeNbug.mainserver.domain.user.dto.response.LoginResponse;
import org.codeNbug.mainserver.domain.user.dto.request.SignupRequest;
import org.codeNbug.mainserver.domain.user.dto.response.SignupResponse;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.codeNbug.mainserver.domain.user.repository.UserRepository;
import org.codeNbug.mainserver.global.exception.globalException.DuplicateEmailException;
import org.codeNbug.mainserver.global.exception.security.AuthenticationFailedException;
import org.codeNbug.mainserver.global.Redis.service.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 서비스
 */
@Service
@RequiredArgsConstructor
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
            throw new DuplicateEmailException("이미 존재하는 이메일입니다.");
        }

        // 사용자 엔티티 생성 및 저장
        User user = request.toEntity(passwordEncoder);
        User savedUser = userRepository.save(user);

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
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요.");
        }

        // 토큰 생성
        TokenService.TokenInfo tokenInfo = tokenService.generateTokens(user.getEmail());

        // 응답 반환
        return LoginResponse.of(tokenInfo.getAccessToken(), tokenInfo.getRefreshToken());
    }

    /**
     * 로그아웃 서비스
     *
     * @param refreshToken Refresh Token
     */
    @Transactional
    public void logout(String refreshToken) {
        tokenService.invalidateTokens(refreshToken);
    }
}
