package org.codenbug.user.security.exception;

/**
 * 토큰이 유효하지 않을 때 발생하는 예외
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}