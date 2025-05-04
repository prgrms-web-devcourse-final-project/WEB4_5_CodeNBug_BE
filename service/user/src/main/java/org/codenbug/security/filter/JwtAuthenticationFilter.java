package org.codenbug.security.filter;

import java.io.IOException;

import org.codenbug.common.util.CookieUtil;
import org.codenbug.common.util.JwtConfig;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT 인증 필터
 *
 * SecurityConfig에 정의된 경로에 해당하는 HTTP 요청에 대해 JWT 토큰을 검증하고 인증 처리를 수행
 * 요청 헤더에서 JWT 토큰을 추출하고 유효성을 검사한 후 Spring Security의 인증 컨텍스트에 사용자 정보를 설정
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtConfig jwtConfig;
	private final UserDetailsService userDetailsService;
	private final CookieUtil cookieUtil;

	/**
	 * JwtAuthenticationFilter 생성자
	 *
	 * @param jwtConfig JWT 설정 및 유틸리티
	 * @param userDetailsService 사용자 상세 정보 서비스
	 * @param cookieUtil Cookie 유틸리티
	 */
	public JwtAuthenticationFilter(JwtConfig jwtConfig, UserDetailsService userDetailsService, CookieUtil cookieUtil) {
		this.jwtConfig = jwtConfig;
		this.userDetailsService = userDetailsService;
		this.cookieUtil = cookieUtil;
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

		try {
			String jwt = null;

			// Authorization 헤더에서 JWT 토큰을 확인
			final String authHeader = request.getHeader("Authorization");
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				jwt = authHeader.substring(7);
			}

			// 헤더에서 토큰을 찾지 못했다면 쿠키에서 확인
			if (jwt == null) {
				jwt = cookieUtil.getAccessTokenFromCookie(request);
			}

			// 토큰이 없으면 다음 필터로 진행
			if (jwt == null) {
				filterChain.doFilter(request, response);
				return;
			}

			try {
				final String username = jwtConfig.extractUsername(jwt);

				// 사용자 이름이 존재하고, 현재 SecurityContext에 인증 정보가 없는 경우에만 인증 처리
				if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
					UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

					// JWT 토큰이 유효성 검증
					if (jwtConfig.validateToken(jwt)) {
						UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());
						authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authToken);
					}
				}
			} catch (Exception e) {
				// JWT 토큰 검증 실패 시 인증 컨텍스트를 클리어
				SecurityContextHolder.clearContext();
			}

			filterChain.doFilter(request, response);
		} catch (Exception e) {
			// 예외 발생 시 인증 컨텍스트를 클리어하고 다음 필터로 진행
			SecurityContextHolder.clearContext();
			filterChain.doFilter(request, response);
		}
	}
}