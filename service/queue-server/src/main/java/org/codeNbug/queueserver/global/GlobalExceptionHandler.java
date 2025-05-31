package org.codeNbug.queueserver.global;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * 글로벌 예외 처리 핸들러
 * <p>
 * 애플리케이션 전체에서 발생하는 예외를 처리하고 적절한 응답을 반환합니다.
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 일반 예외 처리
	 * 위에서 처리되지 않은 모든 예외를 처리합니다.
	 *
	 * @param e 예외 객체
	 * @return API 응답
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleAllExceptions(Exception e) {
		e.printStackTrace();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(e.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
		log.error("Access denied: {}", e.getMessage());
		ErrorResponse response = new ErrorResponse(
			HttpStatus.FORBIDDEN.value(),
			"접근 권한이 없습니다.",
			e.getMessage()
		);
		return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
	}

	// 에러 응답 클래스
	private record ErrorResponse(
		int status,
		String message,
		String detail
	) {
	}
}