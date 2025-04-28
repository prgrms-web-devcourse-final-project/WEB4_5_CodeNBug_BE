package org.codeNbug.mainserver.domain.user.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.user.dto.request.LoginRequest;
import org.codeNbug.mainserver.domain.user.dto.response.LoginResponse;
import org.codeNbug.mainserver.domain.user.dto.request.SignupRequest;
import org.codeNbug.mainserver.domain.user.dto.response.SignupResponse;
import org.codeNbug.mainserver.domain.user.service.UserService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codeNbug.mainserver.global.exception.globalException.DuplicateEmailException;
import org.codeNbug.mainserver.global.exception.security.AuthenticationFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    /**
     * 회원가입 API
     *
     * @param request 회원가입 요청 정보
     * @param bindingResult 유효성 검사 결과
     * @return API 응답
     */
    @PostMapping("/signup")
    public ResponseEntity<RsData<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            BindingResult bindingResult) {

        // 입력값 유효성 검사
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
        }

        try {
            // 회원가입 처리
            SignupResponse response = userService.signup(request);
            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "회원가입 성공", response));
        } catch (DuplicateEmailException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RsData<>("409-CONFLICT", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 로그인 API
     *
     * @param request 로그인 요청 정보
     * @param bindingResult 유효성 검사 결과
     * @param response HTTP 응답 객체 (쿠키 설정용)
     * @return API 응답
     */
    @PostMapping("/login")
    public ResponseEntity<RsData<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            BindingResult bindingResult,
            HttpServletResponse response) {

        // 입력값 유효성 검사
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new RsData<>("400-BAD_REQUEST", "데이터 형식이 잘못되었습니다."));
        }

        try {
            // 로그인 처리
            LoginResponse loginResponse = userService.login(request);
            
            // 쿠키에 토큰 설정
            Cookie accessTokenCookie = new Cookie("accessToken", loginResponse.getAccessToken());
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setSecure(true); // HTTPS에서만 전송
            accessTokenCookie.setAttribute("SameSite", "None"); // 크로스 사이트 요청 허용
            
            Cookie refreshTokenCookie = new Cookie("refreshToken", loginResponse.getRefreshToken());
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setPath("/auth/refresh");
            refreshTokenCookie.setSecure(true);
            refreshTokenCookie.setAttribute("SameSite", "None");
            
            response.addCookie(accessTokenCookie);
            response.addCookie(refreshTokenCookie);
            
            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "로그인 성공", loginResponse));
            
        } catch (AuthenticationFailedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new RsData<>("401-UNAUTHORIZED", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
        }
    }
}
