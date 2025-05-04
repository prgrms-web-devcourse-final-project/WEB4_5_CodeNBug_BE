package org.codeNbug.mainserver.domain.user.service;

import org.codeNbug.mainserver.domain.user.dto.request.LoginRequest;
import org.codeNbug.mainserver.domain.user.dto.request.SignupRequest;
import org.codeNbug.mainserver.domain.user.dto.request.UserUpdateRequest;
import org.codeNbug.mainserver.domain.user.dto.response.LoginResponse;
import org.codeNbug.mainserver.domain.user.dto.response.SignupResponse;
import org.codeNbug.mainserver.domain.user.dto.response.UserProfileResponse;
import org.codeNbug.mainserver.global.Redis.service.TokenService;
import org.codeNbug.mainserver.global.exception.globalException.DuplicateEmailException;
import org.codenbug.security.exception.AuthenticationFailedException;
import org.codenbug.user.entity.User;
import org.codenbug.user.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

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
	 * 로그아웃 처리
	 * 토큰을 블랙리스트에 추가하고 Redis에서 RefreshToken을 삭제합니다.
	 *
	 * @param accessToken  액세스 토큰
	 * @param refreshToken 리프레시 토큰
	 */
	@Transactional
	public void logout(String accessToken, String refreshToken) {
		if (accessToken == null || refreshToken == null) {
			throw new AuthenticationFailedException("인증 정보가 필요합니다.");
		}

		// RefreshToken 삭제
		String email = tokenService.getEmailFromToken(refreshToken);
		tokenService.deleteRefreshToken(email);

		// AccessToken 블랙리스트 처리
		long expirationTime = tokenService.getExpirationTimeFromToken(accessToken);
		tokenService.addToBlacklist(accessToken, expirationTime);
	}

	/**
	 * 회원 탈퇴 처리
	 * 사용자 정보를 삭제하고 토큰을 무효화합니다.
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

		// 사용자 존재 여부 확인
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		// 사용자 삭제
		userRepository.delete(user);

		// RefreshToken 삭제
		tokenService.deleteRefreshToken(email);

		// AccessToken 블랙리스트 처리
		long expirationTime = tokenService.getExpirationTimeFromToken(accessToken);
		tokenService.addToBlacklist(accessToken, expirationTime);
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
