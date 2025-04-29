package org.codeNbug.mainserver.global.exception.globalException;

import org.codeNbug.mainserver.global.dto.RsData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends RuntimeException {
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<RsData> handleIllegalArgumentException(IllegalArgumentException e) {
		log.error("[IllegalArgumentException] {}", e.getMessage(), e);
		RsData response = new RsData(
			"404",
			e.getMessage() != null ? e.getMessage() : "요청한 리소스를 찾을 수 없습니다."
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@SuppressWarnings("checkstyle:ParameterName")
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<RsData> handleIllegalStateException(IllegalStateException e) {
		log.error("[IllegalStateException] {}", e.getMessage(), e);
		RsData response = new RsData(
			"409",
			e.getMessage() != null ? e.getMessage() : "요청을 처리할 수 없습니다."
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}
}
