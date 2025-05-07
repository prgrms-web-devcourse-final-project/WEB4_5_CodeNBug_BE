package org.codeNbug.mainserver.global.Redis.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeNbug.mainserver.global.exception.security.InvalidTokenException;
import org.codeNbug.mainserver.global.Redis.repository.RedisRepository;
import org.codeNbug.mainserver.global.util.JwtConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;
import java.util.Date;

/**
 * 토큰 관리를 위한 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtConfig jwtConfig;
    private final RedisRepository redisRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String EMAIL_PREFIX = "email:";

    /**
     * Access Token과 Refresh Token을 생성하고, Refresh Token을 Redis에 저장
     * 일반 사용자용 토큰 생성
     *
     * @param email 사용자 이메일
     * @return 생성된 토큰 정보
     */
    @Transactional
    public TokenInfo generateTokens(String email) {
        log.info(">> 토큰 생성 시작: 사용자={}", email);
        
        String accessToken = jwtConfig.generateAccessToken(email);
        log.debug(">> 액세스 토큰 생성 완료: {}", accessToken);
        
        String refreshToken = jwtConfig.generateRefreshToken(email);
        log.debug(">> 리프레시 토큰 생성 완료: {}", refreshToken);

        // Refresh Token을 Redis에 저장
        redisRepository.saveRefreshToken(refreshToken, email, jwtConfig.getRefreshTokenExpiration());
        log.info(">> 리프레시 토큰 Redis 저장 완료: 사용자={}, 만료시간={}ms", 
                email, jwtConfig.getRefreshTokenExpiration());

        return TokenInfo.of(accessToken, refreshToken);
    }

    /**
     * SNS 사용자용 Access Token과 Refresh Token을 생성하고, Refresh Token을 Redis에 저장
     *
     * @param socialId 소셜 ID
     * @param provider 제공자 (KAKAO, GOOGLE 등)
     * @return 생성된 토큰 정보
     */
    @Transactional
    public TokenInfo generateTokensForSnsUser(String socialId, String provider) {
        // socialId:provider 형식의 식별자 생성
        String identifier = socialId + ":" + provider;
        log.info(">> SNS 사용자 토큰 생성 시작: 식별자={}", identifier);
        
        String accessToken = jwtConfig.generateAccessToken(identifier);
        log.debug(">> SNS 사용자 액세스 토큰 생성 완료: {}", accessToken);
        
        String refreshToken = jwtConfig.generateRefreshToken(identifier);
        log.debug(">> SNS 사용자 리프레시 토큰 생성 완료: {}", refreshToken);

        // Refresh Token을 Redis에 저장
        redisRepository.saveRefreshToken(refreshToken, identifier, jwtConfig.getRefreshTokenExpiration());
        log.info(">> SNS 사용자 리프레시 토큰 Redis 저장 완료: 식별자={}, 만료시간={}ms", 
                identifier, jwtConfig.getRefreshTokenExpiration());

        return TokenInfo.of(accessToken, refreshToken);
    }

    /**
     * Refresh Token을 검증하고 새로운 토큰을 발급
     *
     * @param refreshToken Refresh Token
     * @return 토큰 정보 (Access Token과 필요한 경우 새 Refresh Token)
     * @throws InvalidTokenException Refresh Token이 유효하지 않은 경우
     */
    @Transactional
    public TokenInfo refreshTokens(String refreshToken) {
        log.info(">> 토큰 갱신 요청: refreshToken={}", refreshToken);
        
        // Redis에서 Refresh Token으로 식별자 조회 및 JWT 유효성 검증
        String identifier = redisRepository.getIdentifierByRefreshToken(refreshToken);
        log.debug(">> Redis에서 조회된 식별자: {}", identifier);
        
        boolean isJwtValid = jwtConfig.validateToken(refreshToken);
        log.debug(">> JWT 유효성 검증 결과: {}", isJwtValid);

        // Redis에 토큰이 없거나 JWT가 유효하지 않은 경우
        if (identifier == null || !isJwtValid) {
            log.warn(">> 리프레시 토큰 검증 실패: identifier={}, isJwtValid={}", identifier, isJwtValid);
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        // Access Token만 새로 발급
        String newAccessToken = jwtConfig.generateAccessToken(identifier);
        log.info(">> 새 액세스 토큰 발급 완료: 사용자={}", identifier);
        
        return TokenInfo.of(newAccessToken, refreshToken);
    }

    /**
     * 로그아웃 시 토큰을 무효화
     *
     * @param refreshToken Refresh Token
     */
    @Transactional
    public void invalidateTokens(String refreshToken) {
        log.info(">> 토큰 무효화 요청: refreshToken={}", refreshToken);
        redisRepository.deleteRefreshToken(refreshToken);
        log.info(">> 리프레시 토큰 삭제 완료");
    }

    /**
     * 토큰을 블랙리스트에 추가
     */
    public void addToBlacklist(String token, long expirationTime) {
        String key = BLACKLIST_PREFIX + token;
        log.info(">> 토큰 블랙리스트 추가: 만료시간={}ms", expirationTime);
        redisTemplate.opsForValue().set(key, "blacklisted", expirationTime, TimeUnit.MILLISECONDS);
        log.debug(">> 블랙리스트 키 설정 완료: {}", key);
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        boolean result = Boolean.TRUE.equals(redisTemplate.hasKey(key));
        log.debug(">> 블랙리스트 확인: key={}, 결과={}", key, result);
        return result;
    }

    /**
     * RefreshToken 삭제
     * 일반 사용자 및 SNS 사용자 모두 지원합니다.
     * 
     * @param identifier 사용자 식별자 (이메일 또는 socialId:provider)
     */
    public void deleteRefreshToken(String identifier) {
        log.info(">> 리프레시 토큰 삭제 요청: 식별자={}", identifier);
        
        // 일반적인 EMAIL_PREFIX 처리 로직은 일반 사용자용
        // 기존 로직에서는 key가 EMAIL_PREFIX + email 형태
        // 하지만 RefreshToken은 refresh_token:{token} 형태로 저장되며 값이 식별자
        // 따라서 RefreshToken을 먼저 찾아서 삭제하는 것이 더 정확함
        
        try {
            // Redis에 저장된 모든 RefreshToken 중에서 value가 identifier인 것을 찾아서 삭제
            String key = EMAIL_PREFIX + identifier;
            log.debug(">> Redis 키 삭제 시도: {}", key);
            Boolean deleted = redisTemplate.delete(key);
            log.info(">> Redis 키 삭제 결과: {}, 키={}", deleted != null && deleted ? "성공" : "실패/없음", key);
        } catch (Exception e) {
            log.error(">> 리프레시 토큰 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 토큰에서 Subject(식별자) 추출
     */
    public String getSubjectFromToken(String token) {
        try {
            String subject = jwtConfig.extractSubject(token);
            log.debug(">> 토큰에서 Subject 추출: {}", subject);
            return subject;
        } catch (Exception e) {
            log.error(">> 토큰에서 Subject 추출 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 토큰의 남은 만료 시간 계산
     */
    public long getExpirationTimeFromToken(String token) {
        try {
            long expirationTime = jwtConfig.getRemainingTime(token);
            log.debug(">> 토큰 만료 시간 계산: {}ms", expirationTime);
            return expirationTime;
        } catch (Exception e) {
            log.error(">> 토큰 만료 시간 계산 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 토큰 검증 및 식별자 일치 확인
     * 일반 사용자와 SNS 사용자 모두 지원합니다.
     * 
     * @param token 검증할 토큰
     * @param expectedIdentifier 예상되는 식별자
     * @return 토큰이 유효하고 식별자가 일치하면 true
     */
    public boolean validateTokenAndIdentifier(String token, String expectedIdentifier) {
        try {
            if (!jwtConfig.validateToken(token)) {
                log.warn(">> 토큰 검증 실패: 토큰이 유효하지 않음");
                return false;
            }
            
            String tokenIdentifier = getSubjectFromToken(token);
            boolean matches = tokenIdentifier.equals(expectedIdentifier);
            
            if (!matches) {
                log.warn(">> 식별자 불일치: 토큰 식별자={}, 예상 식별자={}", tokenIdentifier, expectedIdentifier);
            }
            
            return matches;
        } catch (Exception e) {
            log.error(">> 토큰 검증 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 토큰 정보를 담는 내부 클래스
     */
    @RequiredArgsConstructor
    @Getter
    public static class TokenInfo {
        private final String accessToken;
        private final String refreshToken;

        public static TokenInfo of(String accessToken, String refreshToken) {
            return new TokenInfo(accessToken, refreshToken);
        }
    }
} 