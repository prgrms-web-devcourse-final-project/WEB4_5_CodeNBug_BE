package org.codenbug.user.security.config;

import java.util.Arrays;
import java.util.List;

import org.codenbug.user.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.extern.slf4j.Slf4j;

/**
 * 스프링 시큐리티 설정 클래스
 * <p>
 * 애플리케이션의 보안 설정을 정의
 * 인증 방식, 접근 제어, 비밀번호 인코딩, JWT 필터 등의 보안 관련 설정을 포함
 * </p>
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final CorsProperties corsProperties;

    /**
     * SecurityConfig 생성자
     *
     * @param jwtAuthenticationFilter JWT 인증 필터
     * @param userDetailsService      사용자 상세 정보 서비스
     * @param corsProperties          CORS 설정 프로퍼티
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, UserDetailsService userDetailsService,
                          CorsProperties corsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.corsProperties = corsProperties;
    }

    /**
     * 비밀번호 인코더 빈을 제공
     *
     * @return BCrypt 알고리즘을 사용하는 비밀번호 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 인증 제공자 빈을 구성
     *
     * @return DaoAuthenticationProvider 인증 제공자
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 인증 매니저 빈을 제공
     *
     * @param config 인증 설정
     * @return 인증 매니저 인스턴스
     * @throws Exception 인증 매니저를 생성하는 과정에서 발생할 수 있는 예외
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 보안 필터 체인을 구성합니다.
     * CSRF 방지, 세션 관리, URL 접근 권한, JWT 필터 등의 보안 설정을 포함
     *
     * @param http HTTP 보안 구성 객체
     * @return 구성된 보안 필터 체인
     * @throws Exception 보안 필터 체인을 구성하는 과정에서 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 인증 없이 접근 가능한 경로 설정
                        .requestMatchers("/api/v1/users/signup", "/api/v1/users/login").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/admin/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/events").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/{id}/seats").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/{id}").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/events/view").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/{id}/tickets/waiting").permitAll()

                        .requestMatchers("/api/v1/email/**").permitAll()
                        .requestMatchers("/api/v1/manager/**").permitAll()
                        .requestMatchers("/api/test/auth/public").permitAll()
                        .requestMatchers("/auth/kakao/**").permitAll()
                        .requestMatchers("/webhook/**").permitAll()
                        // Swagger UI 관련 경로 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 나머지 경로는 인증 필요
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            // authException.printStackTrace();
                            // log.info(request.getCookies().toString() + "\n" + request.getHeaderNames().toString() + "\n");
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":\"401-UNAUTHORIZED\",\"msg\":\"인증 정보가 필요합니다.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":\"403-FORBIDDEN\",\"msg\":\"접근 권한이 없습니다.\"}");
                        }))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정을 제공하는 빈
     *
     * @return CORS 구성 소스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // application.yml에서 설정한 cors.allowed-origins 속성 사용
        List<String> allowedOrigins = corsProperties.getAllowedOrigins();
        log.info("allowedOrigins: {}", allowedOrigins);
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            // Allow all patterns including wildcards by setting setAllowedOriginPatterns instead of setAllowedOrigins
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            // 기본값 설정
            //            configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
} 