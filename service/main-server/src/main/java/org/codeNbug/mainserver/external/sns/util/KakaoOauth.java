package org.codeNbug.mainserver.external.sns.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카카오 OAuth 인증을 처리하는 컴포넌트 클래스
 * SocialOauth 인터페이스를 구현하여 카카오 소셜 로그인 기능을 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOauth implements SocialOauth {

    /**
     * application.yml에서 주입받는 카카오 OAuth 관련 설정값들
     */
    @Value("${sns.kakao.url}")
    private String KAKAO_SNS_BASE_URL;  // 카카오 인증 기본 URL

    @Value("${sns.kakao.client.id}")
    private String KAKAO_SNS_CLIENT_ID;  // 카카오 애플리케이션 클라이언트 ID

    @Value("${sns.kakao.callback.url}")
    private String KAKAO_SNS_CALLBACK_URL;  // 인증 후 콜백받을 URL

    @Value("${sns.kakao.client.secret}")
    private String KAKAO_SNS_CLIENT_SECRET;  // 카카오 애플리케이션 시크릿 키

    @Value("${sns.kakao.token.url}")
    private String KAKAO_SNS_TOKEN_BASE_URL;  // 토큰 요청을 위한 URL

    /**
     * 카카오 로그인 페이지로 리다이렉트할 URL을 생성하는 메소드
     * @return 카카오 로그인 페이지 URL (파라미터 포함)
     */
    @Override
    public String getOauthRedirectURL() {
        // OAuth 인증에 필요한 파라미터들을 Map에 저장
        Map<String, Object> params = new HashMap<>();
        params.put("response_type", "code");  // OAuth 인증 코드 요청
        params.put("client_id", KAKAO_SNS_CLIENT_ID);  // 클라이언트 ID
        params.put("redirect_uri", KAKAO_SNS_CALLBACK_URL);  // 콜백 URL
        params.put("scope", "account_email");  // 이메일 정보 요청 권한 추가

        // Map의 파라미터들을 URL 쿼리 스트링 형식으로 변환
        String parameterString = params.entrySet().stream()
                .map(x -> x.getKey() + "=" + x.getValue())
                .collect(Collectors.joining("&"));

        log.info(params.toString());
        // 최종 리다이렉트 URL 반환
        String returnUrl = KAKAO_SNS_BASE_URL + "?" + parameterString;
        log.info(returnUrl);
        return KAKAO_SNS_BASE_URL + "?" + parameterString;
    }

    /**
     * 인증 코드를 이용하여 카카오 액세스 토큰을 요청하는 메소드
     * @param code 카카오 인증 후 받은 인증 코드
     * @return 카카오 서버의 응답 (액세스 토큰 포함) 또는 에러 메시지
     */
    @Override
    public String requestAccessToken(String code) {
        // RestTemplate 인스턴스 생성 (HTTP 요청을 위한 스프링 유틸리티)
        RestTemplate restTemplate = new RestTemplate();

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);  // Content-Type 설정

        // 토큰 요청에 필요한 파라미터 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);  // 인증 코드
        params.add("client_id", KAKAO_SNS_CLIENT_ID);  // 클라이언트 ID
        params.add("client_secret", KAKAO_SNS_CLIENT_SECRET);  // 클라이언트 시크릿
        params.add("redirect_uri", KAKAO_SNS_CALLBACK_URL);  // 리다이렉트 URI
        params.add("grant_type", "authorization_code");  // 인증 타입

        // HTTP 요청 엔티티 생성 (헤더와 바디 포함)
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        // POST 요청 실행 및 응답 수신
        ResponseEntity<String> responseEntity =
                restTemplate.postForEntity(KAKAO_SNS_TOKEN_BASE_URL, requestEntity, String.class);

        // 응답 상태 확인 및 결과 반환
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return responseEntity.getBody();  // 성공 시 응답 바디 반환
        }
        return "카카오 로그인 요청 처리 실패";  // 실패 시 에러 메시지 반환
    }

    /**
     * 카카오 로그아웃 REST API 호출
     * @param accessToken 카카오 액세스 토큰
     */
    public void kakaoLogout(String accessToken) {
        String logoutUrl = "https://kapi.kakao.com/v1/user/logout";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(logoutUrl, requestEntity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info(">> 카카오 로그아웃 API 호출 성공: {}", response.getBody());
            } else {
                log.warn(">> 카카오 로그아웃 API 호출 실패: status={}, body={}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error(">> 카카오 로그아웃 API 호출 중 예외 발생: {}", e.getMessage(), e);
        }
    }
}