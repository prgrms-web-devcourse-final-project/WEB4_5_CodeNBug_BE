package org.codenbug.user.global.util;

import org.codenbug.user.global.exception.security.AuthenticationFailedException;
import org.codenbug.user.global.security.service.CustomUserDetails;
import org.codenbug.user.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 보안 관련 유틸리티 클래스
 * 현재 인증된 사용자의 정보를 쉽게 가져올 수 있는 메서드들을 제공합니다.
 */
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityUtil {

	/**
	 * 현재 인증된 사용자의 ID를 반환합니다.
	 * @return 현재 인증된 사용자의 ID
	 * @throws AuthenticationFailedException 인증된 사용자가 없거나 인증 정보를 찾을 수 없는 경우
	 */
	public static Long getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
			|| !(authentication.getPrincipal() instanceof CustomUserDetails)) {
			throw new AuthenticationFailedException("인증된 사용자를 찾을 수 없습니다.");
		}

		return ((CustomUserDetails)authentication.getPrincipal()).getUserId();
	}

	/**
	 * 현재 인증된 사용자의 이메일을 반환합니다.
	 * @return 현재 인증된 사용자의 이메일
	 * @throws AuthenticationFailedException 인증된 사용자가 없거나 인증 정보를 찾을 수 없는 경우
	 */
	public static String getCurrentUserEmail() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
			|| !(authentication.getPrincipal() instanceof CustomUserDetails)) {
			throw new AuthenticationFailedException("인증된 사용자를 찾을 수 없습니다.");
		}

		return ((CustomUserDetails)authentication.getPrincipal()).getUsername();
	}

	/**
	 * 현재 인증된 사용자의 User 엔티티를 반환합니다.
	 * @return 현재 인증된 사용자의 User 엔티티
	 * @throws AuthenticationFailedException 인증된 사용자가 없거나 인증 정보를 찾을 수 없는 경우
	 */
	public static User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
			|| !(authentication.getPrincipal() instanceof CustomUserDetails)) {
			throw new AuthenticationFailedException("인증된 사용자를 찾을 수 없습니다.");
		}

		return ((CustomUserDetails)authentication.getPrincipal()).getUser();
	}
} 