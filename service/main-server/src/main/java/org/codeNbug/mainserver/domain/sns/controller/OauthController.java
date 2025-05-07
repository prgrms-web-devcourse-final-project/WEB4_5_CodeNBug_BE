package org.codeNbug.mainserver.domain.sns.controller;

import org.codeNbug.mainserver.global.dto.RsData;
import org.codenbug.common.util.CookieUtil;
import org.codenbug.user.redis.service.TokenService;
import org.codenbug.user.sns.constant.SocialLoginType;
import org.codenbug.user.sns.dto.AdditionalInfoRequest;
import org.codenbug.user.sns.dto.UserResponse;
import org.codenbug.user.sns.service.OauthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/auth")
@Slf4j
public class OauthController {

    private final OauthService oauthService;
    private final CookieUtil cookieUtil;
    private final TokenService tokenService;

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
            
            // 쿠키 설정 확인을 위해 로그 추가
            log.info(">> 액세스 토큰 쿠키 설정 완료: {}", userResponse.getAccessToken());
            log.info(">> 리프레시 토큰 쿠키 설정 완료: {}", userResponse.getRefreshToken());
            
            // 응답 헤더에 토큰 정보도 포함 (개발 환경에서 디버깅용)
            response.addHeader("X-Access-Token", userResponse.getAccessToken());
            response.addHeader("X-Refresh-Token", userResponse.getRefreshToken());

            // 실제 사용자 정보가 포함된 응답 반환 (디버깅을 위해 실제 토큰 정보도 포함)
            UserResponse responseData = new UserResponse(
                    userResponse.getName(),
                    userResponse.getAccessToken(),
                    userResponse.getRefreshToken(),
                    userResponse.getProvider()
            );
            
            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "소셜 로그인 성공", responseData));

        } catch (Exception e) {
            log.error(">> 소셜 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "소셜 로그인 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * SNS 로그인 사용자의 추가 정보를 업데이트하는 API
     * @param socialId 소셜 로그인 ID
     * @param request 추가 정보 요청 DTO
     * @return API 응답
     */
    @PostMapping("/sns/additional-info/{socialId}")
    public ResponseEntity<RsData<UserResponse>> updateAdditionalInfo(
            @PathVariable String socialId,
            @Valid @RequestBody AdditionalInfoRequest request) {
        
        log.info(">> SNS 사용자 추가 정보 업데이트 요청 :: socialId = {}", socialId);
        
        try {
            var updatedUser = oauthService.updateAdditionalInfo(socialId, request);
            
            // 토큰 정보 생성
            String tokenIdentifier = updatedUser.getSocialId() + ":" + updatedUser.getProvider();
            var tokenInfo = tokenService.generateTokens(tokenIdentifier);
            
            // 응답 데이터 생성
            UserResponse responseData = new UserResponse(
                    updatedUser.getName(),
                    tokenInfo.getAccessToken(),
                    tokenInfo.getRefreshToken(),
                    updatedUser.getProvider()
            );
            
            return ResponseEntity.ok(
                    new RsData<>("200-SUCCESS", "추가 정보 업데이트 성공", responseData));
                    
        } catch (Exception e) {
            log.error(">> 추가 정보 업데이트 실패 :: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RsData<>("500-INTERNAL_SERVER_ERROR", "추가 정보 업데이트 실패"));
        }
    }
}