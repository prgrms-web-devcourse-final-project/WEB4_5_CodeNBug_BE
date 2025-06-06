package org.codeNbug.mainserver.global.exception;

import org.codeNbug.mainserver.global.dto.RsData;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.codeNbug.mainserver.global.exception.globalException.ConflictException;
import org.codeNbug.mainserver.global.exception.globalException.DuplicateEmailException;
import org.codenbug.user.security.exception.AuthenticationFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
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
	 * DuplicateEmailException 처리
	 * 이메일이 중복된 경우 발생하는 예외를 처리합니다.
	 *
	 * @param e 예외 객체
	 * @return API 응답
	 */
	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<RsData<Object>> handleDuplicateEmailException(DuplicateEmailException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(new RsData<>("409-CONFLICT", e.getMessage()));
	}

	/**
	 * AuthenticationFailedException 처리
	 * 인증 실패 시 발생하는 예외를 처리합니다.
	 *
	 * @param e 예외 객체
	 * @return API 응답
	 */
	@ExceptionHandler(AuthenticationFailedException.class)
	public ResponseEntity<RsData<Object>> handleAuthenticationFailedException(AuthenticationFailedException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(new RsData<>("401-UNAUTHORIZED", e.getMessage()));
	}

	/**
	 * IllegalArgumentException 처리
	 * 잘못된 인자 사용 시 발생하는 예외를 처리합니다.
	 *
	 * @param e 예외 객체
	 * @return API 응답
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<RsData<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(new RsData<>("404-NOT_FOUND", e.getMessage()));
	}

	/**
	 * MethodArgumentNotValidException 처리
	 * 요청 바디의 유효성 검사 실패 시 발생하는 예외를 처리합니다.
	 *
	 * @param e 예외 객체
	 * @return API 응답
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<RsData<Object>> handleValidationExceptions(MethodArgumentNotValidException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
	}

	/**
	 * BindException 처리
	 * 폼 데이터 바인딩 실패 시 발생하는 예외를 처리합니다.
	 *
	 * @param e 예외 객체
	 * @return API 응답
	 */
	@ExceptionHandler(BindException.class)
	public ResponseEntity<RsData<Object>> handleBindException(BindException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
	}

	/**
	 * ConstraintViolationException 처리
	 * 제약 조건 위반 시 발생하는 예외를 처리합니다.
	 *
	 * @param e 예외 객체
	 * @return API 응답
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<RsData<Object>> handleConstraintViolationException(ConstraintViolationException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
	}

	/**
	 * MissingServletRequestParameterException 처리
	 * 필수 요청 파라미터가 누락된 경우 발생하는 예외를 처리합니다.
	 *
	 * @param e 예외 객체
	 * @return API 응답
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<RsData<Object>> handleMissingServletRequestParameterException(
		MissingServletRequestParameterException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new RsData<>("400-BAD_REQUEST", "필수 데이터가 누락되었습니다."));
	}

	/**
	 * MethodArgumentTypeMismatchException 처리
	 * 요청 파라미터의 타입이 맞지 않을 때 발생하는 예외를 처리합니다.
	 *
	 * @param e 예외 객체
	 * @return API 응답
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<RsData<Object>> handleMethodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
	}

	/**
	 * BadRequestException 처리
	 * 요청에 대한 조회 데이터를 찾지 못할 때 발생하는 예외를 처라힙니다.
	 *
	 * @param e 예외 객체
	 * @return API 응답
	 */
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<RsData<Object>> handleBadRequestException(BadRequestException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new RsData<>("400-BAD_REQUEST", e.getMessage()));
	}

	/**
	 * ConflictException 처리
	 * 리소스 상태가 요청과 충돌할 때 발생하는 예외를 처리합니다.
	 *
	 * @param e 예외 객체
	 * @return API 응답
	 */
	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<RsData<Object>> handleConflictException(ConflictException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(new RsData<>("409-CONFLICT", e.getMessage()));
	}

	/**
	 * 일반 예외 처리
	 * 위에서 처리되지 않은 모든 예외를 처리합니다.
	 *
	 * @param e 예외 객체
	 * @return API 응답
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<RsData<Object>> handleAllExceptions(Exception e) {
		e.printStackTrace();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new RsData<>("500-INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
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