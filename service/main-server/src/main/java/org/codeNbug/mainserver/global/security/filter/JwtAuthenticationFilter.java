package org.codeNbug.mainserver.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.codeNbug.mainserver.global.Redis.service.TokenService;
import org.codeNbug.mainserver.global.util.JwtConfig;
import org.codeNbug.mainserver.global.util.CookieUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * 
 * SecurityConfig에 정의된 경로에 해당하는 HTTP 요청에 대해 JWT 토큰을 검증하고 인증 처리를 수행
 * 요청 헤더에서 JWT 토큰을 추출하고 유효성을 검사한 후 Spring Security의 인증 컨텍스트에 사용자 정보를 설정
 * 일반 사용자 및 SNS 로그인 사용자 모두 지원합니다.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final UserDetailsService userDetailsService;
    private final CookieUtil cookieUtil;
    private final TokenService tokenService;

    /**
     * JwtAuthenticationFilter 생성자
     * 
     * @param jwtConfig JWT 설정 및 유틸리티
     * @param userDetailsService 사용자 상세 정보 서비스
     * @param cookieUtil Cookie 유틸리티
     * @param tokenService 토큰 서비스
     */
    public JwtAuthenticationFilter(
            JwtConfig jwtConfig, 
            UserDetailsService userDetailsService, 
            CookieUtil cookieUtil,
            TokenService tokenService) {
        this.jwtConfig = jwtConfig;
        this.userDetailsService = userDetailsService;
        this.cookieUtil = cookieUtil;
        this.tokenService = tokenService;
    }

    /**
     * SecurityConfig에 정의된 경로에 해당하는 HTTP 요청에 대해 실행되는 필터 메서드
     * Authorization 헤더를 확인하여 JWT 토큰을 추출하고 인증을 처리
     * 유효한 토큰인 경우 Spring Security의 인증 컨텍스트에 사용자 정보를 설정
     * 
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외 발생 시
     * @throws IOException 입출력 예외 발생 시
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug(">> JWT 인증 필터 시작: URI={}", requestURI);

        try {
            // 토큰 추출 (헤더 또는 쿠키)
            String jwt = extractToken(request);

            // 토큰이 없으면 다음 필터로 진행
            if (jwt == null) {
                log.debug(">> 토큰 없음, 인증 없이 계속 진행: URI={}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            try {
                // 블랙리스트 확인
                if (tokenService.isBlacklisted(jwt)) {
                    log.warn(">> 블랙리스트에 있는 토큰 사용 시도: URI={}", requestURI);
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // 토큰에서 식별자(subject) 추출
                final String identifier = jwtConfig.extractSubject(jwt);
                log.debug(">> 토큰에서 식별자 추출: identifier={}", identifier);

                // 식별자가 존재하고, 현재 SecurityContext에 인증 정보가 없는 경우에만 인증 처리
                if (identifier != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    log.debug(">> 인증 컨텍스트가 비어있음, 사용자 정보 로드: {}", identifier);
                    
                    // SNS 사용자 또는 일반 사용자에 따라 다르게 처리
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(identifier);
                    
                    // JWT 토큰 유효성 검증
                    if (jwtConfig.validateToken(jwt)) {
                        log.debug(">> 토큰 검증 성공, 인증 처리: identifier={}", identifier);
                        
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        log.debug(">> 인증 정보 설정 완료: identifier={}, 권한={}", 
                                identifier, userDetails.getAuthorities());
                    } else {
                        log.warn(">> 토큰 검증 실패: identifier={}", identifier);
                    }
                }
            } catch (Exception e) {
                // JWT 토큰 검증 실패 시 인증 컨텍스트를 클리어
                log.warn(">> 토큰 처리 중 오류 발생: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 예외 발생 시 인증 컨텍스트를 클리어하고 다음 필터로 진행
            log.error(">> 인증 필터 처리 중 예외 발생: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * 요청에서 JWT 토큰을 추출합니다.
     * 
     * @param request HTTP 요청
     * @return 추출된 JWT 토큰, 없으면 null
     */
    private String extractToken(HttpServletRequest request) {
        String jwt = null;
        
        // Authorization 헤더에서 JWT 토큰을 확인
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            log.debug(">> 헤더에서 토큰 추출 성공");
        }
        
        // 헤더에서 토큰을 찾지 못했다면 쿠키에서 확인
        if (jwt == null) {
            jwt = cookieUtil.getAccessTokenFromCookie(request);
            if (jwt != null) {
                log.debug(">> 쿠키에서 토큰 추출 성공");
            }
        }
        
        return jwt;
    }
} 