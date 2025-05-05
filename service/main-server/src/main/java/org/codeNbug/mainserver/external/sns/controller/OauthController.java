package org.codeNbug.mainserver.external.sns.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeNbug.mainserver.external.sns.constant.SocialLoginType;
import org.codeNbug.mainserver.external.sns.dto.UserResponse;
import org.codeNbug.mainserver.external.sns.service.OauthService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codeNbug.mainserver.global.util.CookieUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/auth")
@Slf4j
public class OauthController {

    private final OauthService oauthService;
    private final CookieUtil cookieUtil;

    @GetMapping(value = "/{socialLoginType}")
    public ResponseEntity<String> socialLoginType(
            @PathVariable(name = "socialLoginType") SocialLoginType socialLoginType) {
        log.info(">> 사용자로부터 SNS 로그인 요청을 받음 :: {} Social Login", socialLoginType);
        String redirectURL = oauthService.request(socialLoginType);
        return ResponseEntity.ok(redirectURL);  // 리다이렉션 URL을 응답으로 반환
    }

    @GetMapping(value = "/{socialLoginType}/callback")
    public ResponseEntity<RsData<UserResponse>> callback(
            @PathVariable(name = "socialLoginType") SocialLoginType socialLoginType,
            @RequestParam(name = "code") String code,
            HttpServletResponse response) {

        log.info(">> 소셜 로그인 API 서버로부터 받은 code :: {}", code);

        try {
            // 액세스 토큰을 통해 사용자 정보를 받아오고 JWT 토큰 생성
            UserResponse userResponse = oauthService.requestAccessTokenAndSaveUser(socialLoginType, code);

            // 쿠키에 토큰 저장 (UserController.login 메서드와 유사하게)
            cookieUtil.setAccessTokenCookie(response, userResponse.getAccessToken());
            cookieUtil.setRefreshTokenCookie(response, userResponse.getRefreshToken());

            // 응답에서는 토큰 값을 제외하고 반환 (보안을 위해)
            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "소셜 로그인 성공", UserResponse.ofTokenTypeOnly()));

        } catch (Exception e) {
            log.error(">> 소셜 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "소셜 로그인 처리 중 오류가 발생했습니다."));
        }
    }
}