package org.codenbug.user.sns.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOauth implements SocialOauth {
    @Value("${sns.google.url}")
    private String GOOGLE_SNS_BASE_URL;
    @Value("${sns.google.client.id}")
    private String GOOGLE_SNS_CLIENT_ID;
    @Value("${sns.google.callback.url}")
    private String GOOGLE_SNS_CALLBACK_URL;
    @Value("${sns.google.client.secret}")
    private String GOOGLE_SNS_CLIENT_SECRET;
    @Value("${sns.google.token.url}")
    private String GOOGLE_SNS_TOKEN_BASE_URL;
    
    @Value("#{'${allowed.redirect.domains}'.split(',')}")
    private List<String> allowedDomains;

    @Override
    public String getOauthRedirectURL() {
        Map<String, Object> params = new HashMap<>();
        params.put("scope", "profile email");
        params.put("response_type", "code");
        params.put("client_id", GOOGLE_SNS_CLIENT_ID);
        params.put("redirect_uri", GOOGLE_SNS_CALLBACK_URL);

        String parameterString = params.entrySet().stream()
                .map(x -> x.getKey() + "=" + x.getValue())
                .collect(Collectors.joining("&"));

        return GOOGLE_SNS_BASE_URL + "?" + parameterString;
    }
    
    @Override
    public String getOauthRedirectURL(String redirectUrl) {
        /*// 리다이렉트 URL이 null이거나 유효하지 않으면 기본값 사용
        if (redirectUrl == null || !isValidRedirectUrl(redirectUrl)) {
            log.warn("제공된 리다이렉트 URL이 유효하지 않습니다: {}, 기본값 사용", redirectUrl);
            return getOauthRedirectURL();
        }*/
        
        Map<String, Object> params = new HashMap<>();
        params.put("scope", "profile email");
        params.put("response_type", "code");
        params.put("client_id", GOOGLE_SNS_CLIENT_ID);
        params.put("redirect_uri", redirectUrl);
        
        String parameterString = params.entrySet().stream()
                .map(x -> x.getKey() + "=" + x.getValue())
                .collect(Collectors.joining("&"));
        
        log.info("커스텀 리다이렉트 URL 사용: {}", redirectUrl);
        return GOOGLE_SNS_BASE_URL + "?" + parameterString;
    }

    @Override
    public String requestAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        params.put("client_id", GOOGLE_SNS_CLIENT_ID);
        params.put("client_secret", GOOGLE_SNS_CLIENT_SECRET);
        params.put("redirect_uri", GOOGLE_SNS_CALLBACK_URL);
        params.put("grant_type", "authorization_code");

        ResponseEntity<String> responseEntity =
                restTemplate.postForEntity(GOOGLE_SNS_TOKEN_BASE_URL, params, String.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return responseEntity.getBody();
        }
        return "구글 로그인 요청 처리 실패";
    }
    
    @Override
    public String requestAccessToken(String code, String redirectUrl) {
        /*if (redirectUrl == null || !isValidRedirectUrl(redirectUrl)) {
            log.warn("제공된 리다이렉트 URL이 유효하지 않습니다: {}, 기본값 사용", redirectUrl);
            return requestAccessToken(code);
        }*/
        
        RestTemplate restTemplate = new RestTemplate();
        
        Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        params.put("client_id", GOOGLE_SNS_CLIENT_ID);
        params.put("client_secret", GOOGLE_SNS_CLIENT_SECRET);
        params.put("redirect_uri", redirectUrl);
        params.put("grant_type", "authorization_code");
        
        log.info("커스텀 리다이렉트 URL로 액세스 토큰 요청: {}", redirectUrl);
        ResponseEntity<String> responseEntity =
                restTemplate.postForEntity(GOOGLE_SNS_TOKEN_BASE_URL, params, String.class);
                
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return responseEntity.getBody();
        }
        return "구글 로그인 요청 처리 실패";
    }
    
    /*// 리다이렉트 URL 유효성 검증 메서드
    private boolean isValidRedirectUrl(String redirectUrl) {
        // null 체크
        if (redirectUrl == null) {
            return false;
        }
        
        // 허용된 도메인 목록 검증
        return allowedDomains.stream()
                .anyMatch(redirectUrl::contains);
    }*/
}