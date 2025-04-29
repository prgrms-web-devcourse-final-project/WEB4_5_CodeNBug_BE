package org.codeNbug.mainserver.global.exception;

import jakarta.validation.ConstraintViolationException;
import org.codeNbug.mainserver.global.exception.globalException.DuplicateEmailException;
import org.codeNbug.mainserver.global.exception.security.AuthenticationFailedException;
import org.codeNbug.mainserver.global.dto.RsData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 글로벌 예외 처리 핸들러
 * <p>
 * 애플리케이션 전체에서 발생하는 예외를 처리하고 적절한 응답을 반환합니다.
 * </p>
 */
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
    public ResponseEntity<RsData<Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
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
    public ResponseEntity<RsData<Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
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
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
    }
} 