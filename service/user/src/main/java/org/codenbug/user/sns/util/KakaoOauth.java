package org.codenbug.user.sns.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    
    @Value("#{'${allowed.redirect.domains}'.split(',')}")
    private List<String> allowedDomains;

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
        // params.put("scope", "account_email");  // 이메일 정보 요청 권한 추가

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
     * 카카오 로그인 페이지로 리다이렉트할 URL을 생성하는 메소드 (커스텀 리다이렉트 URL 사용)
     * @param redirectUrl 커스텀 리다이렉트 URL
     * @return 카카오 로그인 페이지 URL (파라미터 포함)
     */
    @Override
    public String getOauthRedirectURL(String redirectUrl) {
        // 리다이렉트 URL이 null인 경우에만 기본값 사용
        if (redirectUrl == null || redirectUrl.trim().isEmpty()) {
            log.warn("리다이렉트 URL이 null이거나 비어있습니다. 기본값 사용");
            return getOauthRedirectURL();
        }
        
        // OAuth 인증에 필요한 파라미터들을 Map에 저장
        Map<String, Object> params = new HashMap<>();
        params.put("response_type", "code");  // OAuth 인증 코드 요청
        params.put("client_id", KAKAO_SNS_CLIENT_ID);  // 클라이언트 ID
        params.put("redirect_uri", redirectUrl);  // 커스텀 콜백 URL
        params.put("scope", "account_email");  // 이메일 정보 요청 권한 추가

        // Map의 파라미터들을 URL 쿼리 스트링 형식으로 변환
        String parameterString = params.entrySet().stream()
                .map(x -> x.getKey() + "=" + x.getValue())
                .collect(Collectors.joining("&"));

        log.info("커스텀 리다이렉트 URL 사용 (모든 도메인 허용): {}", redirectUrl);
        log.info(params.toString());
        // 최종 리다이렉트 URL 반환
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

        log.info("카카오 액세스 토큰 요청 시작 - redirect_uri: {}, client_id: {}", 
                KAKAO_SNS_CALLBACK_URL, KAKAO_SNS_CLIENT_ID);

        try {
            // POST 요청 실행 및 응답 수신
            ResponseEntity<String> responseEntity =
                    restTemplate.postForEntity(KAKAO_SNS_TOKEN_BASE_URL, requestEntity, String.class);

            // 응답 상태 확인 및 결과 반환
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                log.info("카카오 액세스 토큰 요청 성공");
                return responseEntity.getBody();  // 성공 시 응답 바디 반환
            } else {
                log.error("카카오 액세스 토큰 요청 실패 - 응답 코드: {}, 응답 본문: {}", 
                        responseEntity.getStatusCode(), responseEntity.getBody());
                return createErrorResponse("카카오 로그인 요청 처리 실패 - 응답 코드: " + responseEntity.getStatusCode(), 
                                         responseEntity.getBody());
            }
        } catch (Exception e) {
            log.error("카카오 액세스 토큰 요청 중 예외 발생: {}", e.getMessage(), e);
            return createErrorResponse("카카오 로그인 요청 처리 중 예외 발생", e.getMessage());
        }
    }
    
    /**
     * 인증 코드를 이용하여 카카오 액세스 토큰을 요청하는 메서드 (커스텀 리다이렉트 URL 사용)
     * @param code 카카오 인증 후 받은 인증 코드
     * @param redirectUrl 커스텀 리다이렉트 URL
     * @return 카카오 서버의 응답 (액세스 토큰 포함) 또는 에러 메시지
     */
    @Override
    public String requestAccessToken(String code, String redirectUrl) {
        // 리다이렉트 URL이 null인 경우에만 기본값 사용
        if (redirectUrl == null || redirectUrl.trim().isEmpty()) {
            log.warn("리다이렉트 URL이 null이거나 비어있습니다. 기본값 사용");
            return requestAccessToken(code);
        }
        
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
        params.add("redirect_uri", redirectUrl);  // 커스텀 리다이렉트 URI
        params.add("grant_type", "authorization_code");  // 인증 타입

        // HTTP 요청 엔티티 생성 (헤더와 바디 포함)
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        log.info("=== 카카오 액세스 토큰 요청 시작 ===");
        log.info("요청 URL: {}", KAKAO_SNS_TOKEN_BASE_URL);
        log.info("Client ID: {}", KAKAO_SNS_CLIENT_ID);
        log.info("Redirect URI: {}", redirectUrl);
        log.info("Authorization Code: {}...", code.length() > 10 ? code.substring(0, 10) + "..." : code);
        log.info("기본 Callback URL (설정값): {}", KAKAO_SNS_CALLBACK_URL);
        
        // redirect_uri 일치 여부 확인
        if (!redirectUrl.equals(KAKAO_SNS_CALLBACK_URL)) {
            log.warn(">>> redirect_uri 불일치 감지! <<<");
            log.warn("요청된 redirect_uri: {}", redirectUrl);
            log.warn("설정된 callback_url: {}", KAKAO_SNS_CALLBACK_URL);
            log.warn("카카오 개발자 콘솔에 '{}' 가 Redirect URI로 등록되어 있는지 확인하세요!", redirectUrl);
        }
        
        try {
            // POST 요청 실행 및 응답 수신
            ResponseEntity<String> responseEntity =
                    restTemplate.postForEntity(KAKAO_SNS_TOKEN_BASE_URL, requestEntity, String.class);

            // 응답 상태 확인 및 결과 반환
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                log.info("카카오 토큰 요청 성공");
                return responseEntity.getBody();  // 성공 시 응답 바디 반환
            } else {
                log.error("카카오 토큰 요청 실패 - 응답 코드: {}, 응답 본문: {}", 
                        responseEntity.getStatusCode(), responseEntity.getBody());
                
                // 카카오 API 에러 응답 파싱 시도
                String errorResponse = parseKakaoErrorResponse(responseEntity.getBody());
                
                return createErrorResponse("카카오 로그인 요청 처리 실패 - 응답 코드: " + responseEntity.getStatusCode(), 
                                         errorResponse);
            }
        } catch (Exception e) {
            log.error("=== 카카오 토큰 요청 중 예외 발생 ===");
            log.error("예외 타입: {}", e.getClass().getSimpleName());
            log.error("예외 메시지: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("원인: {}", e.getCause().getMessage());
            }
            log.error("=== 카카오 개발자 콘솔 확인 사항 ===");
            log.error("1. Redirect URI 등록 확인: {}", redirectUrl);
            log.error("2. 클라이언트 ID 확인: {}", KAKAO_SNS_CLIENT_ID);
            log.error("3. 클라이언트 시크릿 설정 확인");
            log.error("4. 애플리케이션 상태 확인 (서비스 중인지)");
            
            return createErrorResponse("카카오 로그인 요청 처리 중 예외 발생", e.getMessage());
        }
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

    /**
     * 카카오 연동 해제 REST API 호출
     * 사용자의 토큰과 카카오 계정 간의 연결을 해제합니다.
     * 
     * @param accessToken 카카오 액세스 토큰
     */
    public void kakaoUnlink(String accessToken) {
        String unlinkUrl = "https://kapi.kakao.com/v1/user/unlink";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(unlinkUrl, requestEntity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info(">> 카카오 연동 해제 API 호출 성공: {}", response.getBody());
            } else {
                log.warn(">> 카카오 연동 해제 API 호출 실패: status={}, body={}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error(">> 카카오 연동 해제 API 호출 중 예외 발생: {}", e.getMessage(), e);
            throw new RuntimeException("카카오 연동 해제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    // 리다이렉트 URL 유효성 검증 메서드
    private boolean isValidRedirectUrl(String redirectUrl) {
        // null 체크
        if (redirectUrl == null) {
            log.warn("리다이렉트 URL이 null입니다.");
            return false;
        }
        
        log.info("리다이렉트 URL 검증 중: {}", redirectUrl);
        log.info("모든 도메인 허용 모드 - 유효성 검사 통과");
        
        // 모든 도메인 허용 (기존 도메인 검증 로직 비활성화)
        return true;
        
        /*
        // 기존 도메인 검증 로직 (주석처리)
        log.info("허용된 도메인들: {}", allowedDomains);
        
        // 허용된 도메인 목록 검증
        boolean isValid = allowedDomains.stream()
                .anyMatch(domain -> {
                    boolean matches = redirectUrl.contains(domain);
                    log.debug("도메인 '{}' 검증 - 포함 여부: {}", domain, matches);
                    return matches;
                });
        
        if (!isValid) {
            log.error("유효하지 않은 리다이렉트 URL: {}. 허용된 도메인: {}", redirectUrl, allowedDomains);
        }
        
        return isValid;
        */
    }

    /**
     * 에러 응답을 생성하는 헬퍼 메서드
     * @param errorMessage 에러 메시지
     * @param additionalInfo 추가 정보
     * @return 에러 응답 문자열
     */
    private String createErrorResponse(String errorMessage, String additionalInfo) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", errorMessage);
            errorResponse.put("error_description", additionalInfo != null ? additionalInfo : "");
            
            return objectMapper.writeValueAsString(errorResponse);
        } catch (Exception e) {
            log.error("에러 응답 JSON 생성 실패: {}", e.getMessage(), e);
            // JSON 생성 실패 시 fallback
            return "{\"error\": \"JSON 생성 오류\", \"error_description\": \"에러 응답 생성 중 문제가 발생했습니다.\"}";
        }
    }

    /**
     * 카카오 API 에러 응답을 파싱하여 사용자에게 도움이 되는 메시지를 생성
     * @param responseBody 카카오 API 에러 응답 본문
     * @return 파싱된 에러 메시지
     */
    private String parseKakaoErrorResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return "카카오 API에서 빈 응답을 받았습니다.";
        }
        
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode errorNode = objectMapper.readTree(responseBody);
            
            String error = errorNode.has("error") ? errorNode.get("error").asText() : "unknown_error";
            String errorDescription = errorNode.has("error_description") ? errorNode.get("error_description").asText() : "";
            String errorCode = errorNode.has("error_code") ? errorNode.get("error_code").asText() : "";
            
            StringBuilder message = new StringBuilder();
            message.append("카카오 API 에러 - ");
            message.append("에러: ").append(error);
            
            if (!errorCode.isEmpty()) {
                message.append(" (코드: ").append(errorCode).append(")");
            }
            
            if (!errorDescription.isEmpty()) {
                message.append(", 설명: ").append(errorDescription);
            }
            
            // 일반적인 카카오 에러에 대한 해결 방법 제안
            if ("invalid_grant".equals(error)) {
                message.append("\n해결방법: ");
                if (errorCode.equals("KOE320")) {
                    message.append("1. 카카오 개발자 콘솔에서 Redirect URI 설정 확인, ");
                    message.append("2. authorization code 재사용 여부 확인, ");
                    message.append("3. authorization code 만료(10분) 여부 확인");
                } else {
                    message.append("authorization code 또는 redirect_uri를 확인하세요.");
                }
            } else if ("invalid_client".equals(error)) {
                message.append("\n해결방법: 카카오 개발자 콘솔에서 클라이언트 ID/Secret 설정을 확인하세요.");
            }
            
            log.error("파싱된 카카오 에러 응답: {}", message.toString());
            return message.toString();
            
        } catch (Exception e) {
            log.error("카카오 API 에러 응답 파싱 실패: {}", e.getMessage(), e);
            return "카카오 API 응답: " + responseBody;
        }
    }
}