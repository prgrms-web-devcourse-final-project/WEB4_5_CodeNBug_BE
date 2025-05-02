package org.codenbug.user.global.exception.security;

/**
 * 인증 실패 예외
 */
public class AuthenticationFailedException extends RuntimeException {
	public AuthenticationFailedException(String message) {
		super(message);
	}
} 