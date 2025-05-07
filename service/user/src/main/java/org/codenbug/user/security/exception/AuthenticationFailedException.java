package org.codenbug.user.security.exception;

/**
 * 인증 실패 예외
 */
public class AuthenticationFailedException extends RuntimeException {
	public AuthenticationFailedException(String message) {
		super(message);
	}
}