package org.codenbug.user.user.service;

import org.codenbug.user.global.exception.security.AuthenticationFailedException;
import org.codenbug.user.global.redis.service.TokenService;
import org.codenbug.user.user.dto.request.UserUpdateRequest;
import org.codenbug.user.user.dto.response.UserProfileResponse;
import org.codenbug.user.user.entity.User;
import org.codenbug.user.user.repository.UserRepository;
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
