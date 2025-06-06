package org.codenbug.user.sns.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.codenbug.user.redis.service.TokenService;
import org.codenbug.user.sns.Entity.SnsUser;
import org.codenbug.user.sns.constant.SocialLoginType;
import org.codenbug.user.sns.dto.AdditionalInfoRequest;
import org.codenbug.user.sns.dto.UserResponse;
import org.codenbug.user.sns.repository.SnsUserRepository;
import org.codenbug.user.sns.util.SocialOauth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OauthService {
	private final List<SocialOauth> socialOauthList; // 다양한 소셜 로그인 OAuth 처리 객체
	private final SnsUserRepository snsUserRepository; // 사용자 정보를 저장하는 레포지토리
	private final TokenService tokenService; // JWT 토큰 생성 서비스 추가
	private static final Logger logger = LoggerFactory.getLogger(OauthService.class);

	// 소셜 로그인 요청 URL을 반환하는 메서드
	public String request(SocialLoginType socialLoginType) {
		SocialOauth socialOauth = this.findSocialOauthByType(socialLoginType); // 주어진 소셜 로그인 타입에 맞는 OAuth 객체 찾기
		return socialOauth.getOauthRedirectURL(); // 해당 OAuth 객체의 리디렉션 URL 반환
	}
	
	// 소셜 로그인 요청 URL을 반환하는 메서드 (커스텀 리다이렉트 URL 사용)
	public String request(SocialLoginType socialLoginType, String redirectUrl) {
		SocialOauth socialOauth = this.findSocialOauthByType(socialLoginType); // 주어진 소셜 로그인 타입에 맞는 OAuth 객체 찾기
		if (redirectUrl != null && !redirectUrl.trim().isEmpty()) {
			logger.info(">> 커스텀 리다이렉트 URL을 사용하여 소셜 로그인 요청: {} -> {}", socialLoginType, redirectUrl);
			return socialOauth.getOauthRedirectURL(redirectUrl); // 커스텀 리다이렉트 URL을 사용하여 리디렉션 URL 반환
		}
		return socialOauth.getOauthRedirectURL(); // 기본 리다이렉트 URL 사용
	}

	// 인증 코드로 액세스 토큰을 요청하는 메서드
	public String requestAccessToken(SocialLoginType socialLoginType, String code) {
		SocialOauth socialOauth = this.findSocialOauthByType(socialLoginType); // 주어진 소셜 로그인 타입에 맞는 OAuth 객체 찾기
		return socialOauth.requestAccessToken(code); // 액세스 토큰 요청
	}
	
	// 인증 코드로 액세스 토큰을 요청하는 메서드 (커스텀 리다이렉트 URL 사용)
	public String requestAccessToken(SocialLoginType socialLoginType, String code, String redirectUrl) {
		SocialOauth socialOauth = this.findSocialOauthByType(socialLoginType); // 주어진 소셜 로그인 타입에 맞는 OAuth 객체 찾기
		if (redirectUrl != null && !redirectUrl.trim().isEmpty()) {
			logger.info(">> 커스텀 리다이렉트 URL을 사용하여 액세스 토큰 요청: {} -> {}", socialLoginType, redirectUrl);
			return socialOauth.requestAccessToken(code, redirectUrl); // 커스텀 리다이렉트 URL을 사용하여 액세스 토큰 요청
		}
		return socialOauth.requestAccessToken(code); // 기본 리다이렉트 URL 사용
	}

	// JSON에서 액세스 토큰만 추출하는 메서드
	private String extractAccessTokenFromJson(String accessTokenJson) {
		// 응답이 null이거나 비어있는지 확인
		if (accessTokenJson == null || accessTokenJson.trim().isEmpty()) {
			logger.error(">> 액세스 토큰 응답이 null이거나 비어있습니다.");
			return null;
		}

		// 응답 내용 로깅 (디버깅용)
		logger.info(">> 액세스 토큰 응답 내용: {}", accessTokenJson);

		// 응답이 JSON 형태인지 간단히 확인
		String trimmed = accessTokenJson.trim();
		if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
			logger.error(">> 응답이 JSON 형태가 아닙니다. 응답 내용: {}", accessTokenJson);
			
			// HTML 에러 페이지인지 확인
			if (trimmed.toLowerCase().contains("<html") || trimmed.toLowerCase().contains("<!doctype")) {
				logger.error(">> HTML 에러 페이지가 반환되었습니다. 카카오 OAuth 설정(redirect_uri, client_id 등)을 확인해주세요.");
				throw new RuntimeException("카카오 OAuth 설정 오류: HTML 에러 페이지가 반환되었습니다. redirect_uri와 카카오 애플리케이션 설정을 확인해주세요.");
			}
			
			// 한글 에러 메시지인지 확인
			if (trimmed.contains("카카오") || trimmed.contains("오류") || trimmed.contains("실패")) {
				logger.error(">> 카카오에서 한글 에러 메시지가 반환되었습니다: {}", trimmed);
				throw new RuntimeException("카카오 OAuth 인증 실패: " + trimmed);
			}
			
			throw new RuntimeException("유효하지 않은 응답 형식입니다: " + trimmed);
		}

		// JSON을 파싱하여 액세스 토큰만 추출
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(accessTokenJson);
			
			// access_token 필드 확인
			if (jsonNode.has("access_token")) {
				String accessToken = jsonNode.get("access_token").asText();
				logger.info(">> 액세스 토큰 추출 성공");
				return accessToken;
			} else {
				logger.error(">> JSON 응답에 access_token 필드가 없습니다. 응답: {}", accessTokenJson);
				
				// 에러 정보가 있는지 확인
				if (jsonNode.has("error")) {
					String error = jsonNode.get("error").asText();
					String errorDescription = jsonNode.has("error_description") ? 
						jsonNode.get("error_description").asText() : "설명 없음";
					String errorCode = jsonNode.has("error_code") ? 
						jsonNode.get("error_code").asText() : "";
					
					logger.error(">> OAuth 에러 - error: {}, description: {}, code: {}", error, errorDescription, errorCode);
					
					// 사용자에게 도움이 되는 구체적인 에러 메시지 생성
					String userFriendlyMessage = createUserFriendlyErrorMessage(error, errorDescription, errorCode);
					throw new RuntimeException(userFriendlyMessage);
				}
				
				return null;
			}
		} catch (JsonProcessingException e) {
			logger.error(">> JSON 파싱 실패. 응답 내용: {}, 에러: {}", accessTokenJson, e.getMessage(), e);
			
			// JSON 파싱 에러의 경우 원본 응답을 분석해서 더 유용한 메시지 제공
			if (accessTokenJson.contains("error") && accessTokenJson.contains("invalid_grant")) {
				throw new RuntimeException("카카오 OAuth 인증 실패: authorization code가 유효하지 않거나 만료되었습니다. " +
					"카카오 개발자 콘솔에서 Redirect URI 설정을 확인하거나 새로운 authorization code로 다시 시도해주세요.");
			}
			
			throw new RuntimeException("액세스 토큰 응답 파싱 실패: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 카카오 API 에러를 사용자가 이해하기 쉬운 메시지로 변환
	 */
	private String createUserFriendlyErrorMessage(String error, String errorDescription, String errorCode) {
		StringBuilder message = new StringBuilder("카카오 로그인 실패: ");
		
		switch (error) {
			case "invalid_grant":
				if ("KOE320".equals(errorCode)) {
					message.append("인증 코드를 찾을 수 없습니다. ");
					message.append("다음 사항을 확인해주세요:\n");
					message.append("1. 카카오 개발자 콘솔에서 Redirect URI가 올바르게 설정되었는지 확인\n");
					message.append("2. authorization code가 이미 사용되었는지 확인 (한 번만 사용 가능)\n");
					message.append("3. authorization code가 만료되었는지 확인 (10분 유효)");
				} else {
					message.append("인증 정보가 유효하지 않습니다. ").append(errorDescription);
				}
				break;
			case "invalid_client":
				message.append("카카오 애플리케이션 설정 오류입니다. 클라이언트 ID 또는 Secret을 확인해주세요.");
				break;
			case "invalid_request":
				message.append("요청 형식이 올바르지 않습니다. ").append(errorDescription);
				break;
			case "unauthorized_client":
				message.append("허가되지 않은 클라이언트입니다. 카카오 개발자 콘솔에서 애플리케이션 상태를 확인해주세요.");
				break;
			default:
				message.append(error).append(" - ").append(errorDescription);
		}
		
		if (!errorCode.isEmpty()) {
			message.append(" (에러 코드: ").append(errorCode).append(")");
		}
		
		return message.toString();
	}

	// 액세스 토큰을 사용하여 사용자 정보를 가져오고, 사용자 정보가 있으면 저장하는 메서드
	@Transactional
	public UserResponse requestAccessTokenAndSaveUser(SocialLoginType socialLoginType, String code) {
		// 1. 액세스 토큰을 포함한 JSON 응답을 요청
		String accessTokenJson = this.requestAccessToken(socialLoginType, code);
		logger.info(">> 소셜 로그인 액세스 토큰 응답: {}", accessTokenJson);

		// 2. JSON에서 액세스 토큰만 추출
		String accessToken = extractAccessTokenFromJson(accessTokenJson);

		if (accessToken == null) {
			logger.error(">> 액세스 토큰 추출 실패");
			throw new RuntimeException("access token 추출에 실패했습니다.");
		}
		logger.info(">> 추출된 액세스 토큰: {}", accessToken);

		// 3. 액세스 토큰을 사용해 사용자 정보 요청
		String userInfo = getUserInfo(socialLoginType, accessToken);
		logger.info(">> 소셜 API에서 받은 사용자 정보: {}", userInfo);

		// 4. 사용자 정보를 파싱하여 SnsUser 객체 생성
		SnsUser user = parseUserInfo(userInfo, socialLoginType);
		logger.info(">> 파싱된 사용자 정보: socialId={}, name={}, provider={}, email={}", 
			user.getSocialId(), user.getName(), user.getProvider(), user.getEmail());

		// 5. 기존 사용자 확인 후 처리
		Optional<SnsUser> existingUser = snsUserRepository.findBySocialId(user.getSocialId());
		SnsUser savedUser;

		if (existingUser.isPresent()) {
			// 이미 존재하는 사용자라면 로그인 처리
			SnsUser existing = existingUser.get();
			logger.info(">> 기존 사용자 확인: id={}, socialId={}", existing.getId(), existing.getSocialId());
			existing.setUpdatedAt(new Timestamp(System.currentTimeMillis())); // 수정 시간 갱신
			
			// 필요한 필드 업데이트
			existing.setName(user.getName());
			if (user.getEmail() != null && !user.getEmail().isEmpty()) {
				existing.setEmail(user.getEmail());
			}
			
			try {
				savedUser = snsUserRepository.save(existing);
				logger.info(">> 기존 사용자 정보 업데이트 성공: id={}", savedUser.getId());
			} catch (Exception e) {
				logger.error(">> 기존 사용자 정보 업데이트 실패: {}", e.getMessage(), e);
				throw new RuntimeException("사용자 정보 업데이트 실패: " + e.getMessage(), e);
			}
		} else {
			// 새 사용자라면 저장
			logger.info(">> 신규 사용자 등록 시작: socialId={}, provider={}", user.getSocialId(), user.getProvider());
			user.setCreatedAt(new Timestamp(System.currentTimeMillis())); // 생성 시간 설정
			user.setUpdatedAt(new Timestamp(System.currentTimeMillis())); // 수정 시간 설정
			user.setIsAdditionalInfoCompleted(false); // 추가 정보 미입력 상태로 설정

			try {
				savedUser = snsUserRepository.save(user);
				logger.info(">> 신규 사용자 등록 성공: id={}, socialId={}", savedUser.getId(), savedUser.getSocialId());
			} catch (Exception e) {
				logger.error(">> 신규 사용자 등록 실패: {}", e.getMessage(), e);
				throw new RuntimeException("신규 사용자 등록 실패: " + e.getMessage(), e);
			}
		}

		// 6. SNS 사용자용 JWT 토큰 생성
		// SNS 사용자용 토큰 생성 메서드 사용
		logger.info(">> SNS 사용자 토큰 생성: socialId={}, provider={}", savedUser.getSocialId(), savedUser.getProvider());
		TokenService.TokenInfo tokenInfo = tokenService.generateTokensForSnsUser(
			savedUser.getSocialId(), savedUser.getProvider());
		logger.debug(">> SNS 사용자 토큰 생성 완료: accessToken={}, refreshToken={}",
			tokenInfo.getAccessToken(), tokenInfo.getRefreshToken());

		// 7. 토큰 및 사용자 정보를 포함한 응답 반환
		return new UserResponse(savedUser.getName(), tokenInfo.getAccessToken(),
			tokenInfo.getRefreshToken(), savedUser.getProvider(), savedUser.getSocialId());
	}

	// 액세스 토큰을 사용하여 사용자 정보를 가져오고, 사용자 정보가 있으면 저장하는 메서드 (커스텀 리다이렉트 URL 사용)
	@Transactional
	public UserResponse requestAccessTokenAndSaveUser(SocialLoginType socialLoginType, String code, String redirectUrl) {
		// 1. 액세스 토큰을 포함한 JSON 응답을 요청 (커스텀 리다이렉트 URL 사용)
		String accessTokenJson = this.requestAccessToken(socialLoginType, code, redirectUrl);
		logger.info(">> 소셜 로그인 액세스 토큰 응답 (커스텀 리다이렉트 URL 사용): {}", accessTokenJson);

		// 2. JSON에서 액세스 토큰만 추출
		String accessToken = extractAccessTokenFromJson(accessTokenJson);

		if (accessToken == null) {
			logger.error(">> 액세스 토큰 추출 실패");
			throw new RuntimeException("access token 추출에 실패했습니다.");
		}
		logger.info(">> 추출된 액세스 토큰: {}", accessToken);

		// 3. 액세스 토큰을 사용해 사용자 정보 요청
		String userInfo = getUserInfo(socialLoginType, accessToken);
		logger.info(">> 소셜 API에서 받은 사용자 정보: {}", userInfo);

		// 4. 사용자 정보를 파싱하여 SnsUser 객체 생성
		SnsUser user = parseUserInfo(userInfo, socialLoginType);
		logger.info(">> 파싱된 사용자 정보: socialId={}, name={}, provider={}, email={}", 
			user.getSocialId(), user.getName(), user.getProvider(), user.getEmail());

		// 5. 기존 사용자 확인 후 처리
		Optional<SnsUser> existingUser = snsUserRepository.findBySocialId(user.getSocialId());
		SnsUser savedUser;

		if (existingUser.isPresent()) {
			// 이미 존재하는 사용자라면 로그인 처리
			SnsUser existing = existingUser.get();
			logger.info(">> 기존 사용자 확인: id={}, socialId={}", existing.getId(), existing.getSocialId());
			existing.setUpdatedAt(new Timestamp(System.currentTimeMillis())); // 수정 시간 갱신
			
			// 필요한 필드 업데이트
			existing.setName(user.getName());
			if (user.getEmail() != null && !user.getEmail().isEmpty()) {
				existing.setEmail(user.getEmail());
			}
			
			try {
				savedUser = snsUserRepository.save(existing);
				logger.info(">> 기존 사용자 정보 업데이트 성공: id={}", savedUser.getId());
			} catch (Exception e) {
				logger.error(">> 기존 사용자 정보 업데이트 실패: {}", e.getMessage(), e);
				throw new RuntimeException("사용자 정보 업데이트 실패: " + e.getMessage(), e);
			}
		} else {
			// 새 사용자라면 저장
			logger.info(">> 신규 사용자 등록 시작: socialId={}, provider={}", user.getSocialId(), user.getProvider());
			user.setCreatedAt(new Timestamp(System.currentTimeMillis())); // 생성 시간 설정
			user.setUpdatedAt(new Timestamp(System.currentTimeMillis())); // 수정 시간 설정
			user.setIsAdditionalInfoCompleted(false); // 추가 정보 미입력 상태로 설정

			try {
				savedUser = snsUserRepository.save(user);
				logger.info(">> 신규 사용자 등록 성공: id={}, socialId={}", savedUser.getId(), savedUser.getSocialId());
			} catch (Exception e) {
				logger.error(">> 신규 사용자 등록 실패: {}", e.getMessage(), e);
				throw new RuntimeException("신규 사용자 등록 실패: " + e.getMessage(), e);
			}
		}

		// 6. SNS 사용자용 JWT 토큰 생성
		// SNS 사용자용 토큰 생성 메서드 사용
		logger.info(">> SNS 사용자 토큰 생성: socialId={}, provider={}", savedUser.getSocialId(), savedUser.getProvider());
		TokenService.TokenInfo tokenInfo = tokenService.generateTokensForSnsUser(
			savedUser.getSocialId(), savedUser.getProvider());
		logger.debug(">> SNS 사용자 토큰 생성 완료: accessToken={}, refreshToken={}",
			tokenInfo.getAccessToken(), tokenInfo.getRefreshToken());

		// 7. 토큰 및 사용자 정보를 포함한 응답 반환
		return new UserResponse(savedUser.getName(), tokenInfo.getAccessToken(),
			tokenInfo.getRefreshToken(), savedUser.getProvider(), savedUser.getSocialId());
	}

	// 실제 소셜 로그인 API에서 사용자 정보를 받아오는 메서드 (Google, Kakao)
	private String getUserInfo(SocialLoginType socialLoginType, String accessToken) {
		switch (socialLoginType) {
			case GOOGLE:
				return googleApiCall(accessToken);  // Google API 호출 메서드
			case KAKAO:
				return kakaoApiCall(accessToken);  // Kakao API 호출 메서드
			default:
				throw new IllegalArgumentException("지원되지 않는 소셜 로그인 타입입니다."); // 지원되지 않는 로그인 타입 오류
		}
	}

	// 각 소셜 로그인 제공자별 API 호출 (실제 API 호출 방식은 각 로그인 서비스의 문서를 참조해야 함)
	// 구글 API 호출 시 응답 상태 코드와 메시지 출력
	public String googleApiCall(String accessToken) {
		try {
			// accessToken을 URL 인코딩
			String encodedAccessToken = URLEncoder.encode(accessToken, "UTF-8");
			logger.debug("Encoded access token: {}", encodedAccessToken);

			String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + encodedAccessToken;
			logger.debug("Google API URL: {}", url);

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection)obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", "application/json");

			int responseCode = con.getResponseCode();
			logger.info("Google API response code: {}", responseCode);

			if (responseCode == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				logger.info("Successfully received response from Google API.");
				return response.toString();
			} else {
				// 실패 시 에러 메시지와 상태 코드 출력
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
				String inputLine;
				StringBuffer errorResponse = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					errorResponse.append(inputLine);
				}
				in.close();
				logger.error("Google API call failed with response code: {}, error: {}", responseCode,
					errorResponse.toString());
				throw new RuntimeException("Google API에서 사용자 정보를 가져오는 데 실패했습니다. 응답 코드: " + responseCode + ", 에러 메시지: "
					+ errorResponse.toString());
			}
		} catch (IOException e) {
			logger.error("Google API 호출 중 오류 발생: {}", e.getMessage(), e);
			throw new RuntimeException("Google API 호출 중 오류 발생", e);
		}
	}

	private String kakaoApiCall(String accessToken) {
		try {
			String url = "https://kapi.kakao.com/v2/user/me";
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection)obj.openConnection();
			con.setRequestMethod("GET");
			// Kakao의 경우 Authorization 헤더에 "Bearer" 토큰을 설정해야 함.
			con.setRequestProperty("Authorization", "Bearer " + accessToken);

			int responseCode = con.getResponseCode();
			if (responseCode == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				return response.toString();
			} else {
				throw new RuntimeException("Kakao API에서 사용자 정보를 가져오는 데 실패했습니다. 응답 코드: " + responseCode);
			}
		} catch (IOException e) {
			throw new RuntimeException("Kakao API 호출 중 오류 발생", e);
		}
	}

	// 사용자 정보를 파싱하여 SnsUser 객체 생성
	private SnsUser parseUserInfo(String userInfo, SocialLoginType socialLoginType) {
		JsonObject jsonObject = JsonParser.parseString(userInfo).getAsJsonObject();

		// socialId와 name을 소셜 로그인 타입별로 분리
		String socialId = "";
		String name = "";
		String email = "";

		if (socialLoginType == SocialLoginType.GOOGLE) {
			socialId = jsonObject.get("sub").getAsString(); // Google은 "sub"를 ID로 사용
			name = jsonObject.get("name").getAsString();    // Google에서 제공하는 이름
			if (jsonObject.has("email")) {
				email = jsonObject.get("email").getAsString(); // Google에서 제공하는 이메일
			}
		} else if (socialLoginType == SocialLoginType.KAKAO) {
			socialId = jsonObject.get("id").getAsString(); // Kakao는 "id"를 사용자 ID로 사용
			name = jsonObject.getAsJsonObject("properties").get("nickname").getAsString(); // Kakao에서 제공하는 nickname

			// Kakao는 'kakao_account' 객체 안에 'email' 필드가 있음
			if (jsonObject.has("kakao_account") &&
				jsonObject.getAsJsonObject("kakao_account").has("email")) {
				email = jsonObject.getAsJsonObject("kakao_account").get("email").getAsString();
			}
		}

		// SnsUser 객체에 정보 세팅
		SnsUser user = new SnsUser();
		user.setSocialId(socialId);             // 소셜 ID 설정
		user.setName(name);                     // 사용자의 이름 설정
		user.setProvider(socialLoginType.name()); // 로그인 제공자 설정
		user.setEmail(email);                   // 이메일 설정
		user.setRole("ROLE_USER");              // 기본 역할을 ROLE_USER로 설정
		return user;
	}

	// 주어진 소셜 로그인 타입에 맞는 OAuth 객체를 찾는 메서드
	private SocialOauth findSocialOauthByType(SocialLoginType socialLoginType) {
		return socialOauthList.stream()
			.filter(x -> x.type() == socialLoginType)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("알 수 없는 SocialLoginType 입니다.")); // 지원되지 않는 소셜 로그인 타입 오류
	}

	/**
	 * SNS 로그인 사용자의 추가 정보를 업데이트하는 메서드
	 * @param socialId 소셜 로그인 ID
	 * @param request 추가 정보 요청 DTO
	 * @return 업데이트된 사용자 정보
	 */
	@Transactional
	public SnsUser updateAdditionalInfo(String socialId, AdditionalInfoRequest request) {
		SnsUser user = snsUserRepository.findBySocialId(socialId)
			.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

		user.setAge(request.getAge());
		user.setSex(request.getSex());
		user.setPhoneNum(request.getPhoneNum());
		user.setLocation(request.getLocation());
		user.setIsAdditionalInfoCompleted(true);
		user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

		return snsUserRepository.save(user);
	}
}