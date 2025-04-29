package org.codeNbug.mainserver.global.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

/**
 * JWT 설정 및 유틸리티 클래스
 * JWT 토큰 생성, 검증, 정보 추출 등의 기능을 제공
 * application.yml에 정의된 jwt.secret과 jwt.expiration 속성을 사용
 */
@Component
public class JwtConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration:1800000}") // 30분 (밀리초)
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}") // 7일 (밀리초)
    private long refreshTokenExpiration;

    /**
     * JWT 서명에 사용할 키를 생성
     *
     * @return 서명용 암호화 키
     */
    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token을 생성
     * 
     * @param username 토큰에 포함될 사용자 이름
     * @return 생성된 Access Token 문자열
     */
    public String generateAccessToken(String username) {
        return generateToken(username, accessTokenExpiration);
    }

    /**
     * Refresh Token을 생성
     * 
     * @param username 토큰에 포함될 사용자 이름
     * @return 생성된 Refresh Token 문자열
     */
    public String generateRefreshToken(String username) {
        return generateToken(username, refreshTokenExpiration);
    }

    /**
     * JWT 토큰을 생성
     * 
     * @param username 토큰에 포함될 사용자 이름
     * @param expiration 만료 시간 (밀리초)
     * @return 생성된 JWT 토큰 문자열
     */
    private String generateToken(String username, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * JWT 토큰에서 사용자 이름을 추출
     * 
     * @param token 검증할 JWT 토큰
     * @return 토큰에서 추출한 사용자 이름
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * JWT 토큰에서 만료 시간을 추출
     * 
     * @param token 검증할 JWT 토큰
     * @return 토큰의 만료 시간
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * JWT 토큰에서 특정 클레임을 추출
     * 
     * @param token 검증할 JWT 토큰
     * @param claimsResolver 클레임 리졸버 함수
     * @return 추출된 클레임 값
     * @param <T> 반환될 클레임의 타입
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * JWT 토큰에서 모든 클레임을 추출
     * 
     * @param token 검증할 JWT 토큰
     * @return 모든 클레임을 포함하는 Claims 객체
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * JWT 토큰이 만료되었는지 확인
     * 
     * @param token 검증할 JWT 토큰
     * @return 토큰 만료 여부 (만료된 경우 true)
     */
    public boolean isTokenExpired(String token) {
        final Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    /**
     * JWT 토큰의 유효성을 검증
     * 
     * @param token 검증할 JWT 토큰
     * @return 토큰 유효성 여부 (유효한 경우 true)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Refresh Token의 만료 시간을 반환
     *
     * @return Refresh Token의 만료 시간 (밀리초)
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
} 