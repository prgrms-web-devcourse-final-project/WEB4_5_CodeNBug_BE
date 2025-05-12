package org.codeNbug.mainserver.domain.admin.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codeNbug.mainserver.domain.admin.dto.request.AdminLoginRequest;
import org.codeNbug.mainserver.domain.admin.dto.request.AdminSignupRequest;
import org.codeNbug.mainserver.domain.admin.dto.response.AdminLoginResponse;
import org.codeNbug.mainserver.domain.admin.dto.response.AdminSignupResponse;
import org.codeNbug.mainserver.domain.admin.service.AdminService;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.security.annotation.RoleRequired;
import org.codenbug.user.security.exception.AuthenticationFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 컨트롤러
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;
    
    /**
     * 관리자 대시보드 페이지
     */
    @RoleRequired(UserRole.ADMIN)
    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        // 인증 정보 확인 로직 (필요 시 구현)
        log.info(">> 관리자 대시보드 페이지 요청");
        
        // 쿠키에서 토큰 확인
        Cookie[] cookies = request.getCookies();
        boolean hasToken = false;
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    log.info(">> 액세스 토큰이 존재합니다: {}", cookie.getValue());
                    hasToken = true;
                    break;
                }
            }
        }
        
        if (!hasToken) {
            log.warn(">> 액세스 토큰이 없습니다. 인증 정보를 확인하세요.");
        }
        
        model.addAttribute("welcomeMessage", "관리자 대시보드에 오신 것을 환영합니다!");
        return "admin/dashboard";
    }
    
    /**
     * 관리자 로그인 페이지
     */
    @GetMapping("/login")
    public String loginPage(Model model) {
        log.info(">> 관리자 로그인 페이지 요청");
        model.addAttribute("adminLoginRequest", new AdminLoginRequest());
        return "admin/login";
    }
    
    /**
     * 관리자 로그인 처리
     */
    @PostMapping("/login")
    public String login(@ModelAttribute AdminLoginRequest request,
                      BindingResult bindingResult,
                      HttpServletResponse response,
                      RedirectAttributes redirectAttributes,
                      @RequestParam(required = false) String email,
                      @RequestParam(required = false) String password) {
        
        log.info(">> 관리자 로그인 요청: 이메일={}", email);
        
        // AdminLoginRequest가 null인 경우 수동으로 객체 생성
        if (request == null || (request.getEmail() == null && email != null)) {
            request = AdminLoginRequest.builder()
                    .email(email)
                    .password(password)
                    .build();
            log.info(">> 수동으로 AdminLoginRequest 생성: 이메일={}", email);
        }
        
        // 유효성 검사 실패 시 로그인 페이지로 리다이렉트
        if (bindingResult.hasErrors()) {
            log.warn(">> 로그인 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            return "admin/login";
        }
        
        try {
            // 로그인 처리
            AdminLoginResponse loginResponse = adminService.login(request);
            
            // 토큰을 쿠키에 저장
            Cookie accessTokenCookie = new Cookie("accessToken", loginResponse.getAccessToken());
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(3600); // 1시간
            
            Cookie refreshTokenCookie = new Cookie("refreshToken", loginResponse.getRefreshToken());
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge(86400); // 24시간
            
            response.addCookie(accessTokenCookie);
            response.addCookie(refreshTokenCookie);
            
            log.info(">> 관리자 로그인 성공: 이메일={}", email);
            return "redirect:/admin/dashboard";
            
        } catch (AuthenticationFailedException e) {
            log.error(">> 관리자 로그인 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "admin/login";
        } catch (Exception e) {
            log.error(">> 관리자 로그인 중 예외 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "로그인 처리 중 오류가 발생했습니다.");
            return "admin/login";
        }
    }
    
    /**
     * 관리자 회원가입 페이지
     */
    @GetMapping("/signup")
    public String signupPage(Model model) {
        log.info(">> 관리자 회원가입 페이지 요청");
        model.addAttribute("adminSignupRequest", new AdminSignupRequest());
        return "admin/signup";
    }
    
    /**
     * 관리자 회원가입 처리
     */
    @PostMapping("/signup")
    public String signup(@ModelAttribute AdminSignupRequest request,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes,
                        Model model,
                        @RequestParam(required = false) String email,
                        @RequestParam(required = false) String password,
                        @RequestParam(required = false) String name,
                        @RequestParam(required = false) Integer age,
                        @RequestParam(required = false) String sex,
                        @RequestParam(required = false) String phoneNum,
                        @RequestParam(required = false) String location) {
        
        log.info(">> 관리자 회원가입 요청: 이메일={}, 이름={}", email, name);
        
        // AdminSignupRequest가 null인 경우 수동으로 객체 생성
        if (request == null || (request.getEmail() == null && email != null)) {
            request = AdminSignupRequest.builder()
                    .email(email)
                    .password(password)
                    .name(name)
                    .age(age)
                    .sex(sex)
                    .phoneNum(phoneNum)
                    .location(location)
                    .build();
            log.info(">> 수동으로 AdminSignupRequest 생성: {}", request);
        }
        
        // 유효성 검사 실패 시 회원가입 페이지로 리다이렉트
        if (bindingResult.hasErrors()) {
            log.warn(">> 회원가입 폼 유효성 검사 실패: {}", bindingResult.getAllErrors());
            return "admin/signup";
        }
        
        try {
            // 회원가입 처리
            log.info(">> AdminService.signup 호출 전");
            AdminSignupResponse signupResponse = adminService.signup(request);
            log.info(">> 관리자 회원가입 성공: id={}, 이메일={}", signupResponse.getId(), signupResponse.getEmail());
            
            redirectAttributes.addFlashAttribute("successMessage", "관리자 계정이 성공적으로 생성되었습니다. 로그인해주세요.");
            return "redirect:/admin/login";
            
        } catch (Exception e) {
            log.error(">> 관리자 회원가입 실패: {}", e.getMessage(), e);
            model.addAttribute("adminSignupRequest", request); // 입력값 유지
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/signup";
        }
    }
    
    /**
     * 관리자 로그아웃 처리
     */
    @GetMapping("/logout")
    @RoleRequired(UserRole.ADMIN)
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        log.info(">> 관리자 로그아웃 요청");
        
        // 쿠키에서 토큰 추출
        String accessToken = null;
        String refreshToken = null;
        
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                } else if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }
        
        // 토큰이 있으면 로그아웃 처리
        if (accessToken != null && refreshToken != null) {
            try {
                adminService.logout(accessToken, refreshToken);
                log.info(">> 관리자 로그아웃 성공");
            } catch (Exception e) {
                log.error(">> 관리자 로그아웃 처리 중 오류 발생: {}", e.getMessage(), e);
            }
        } else {
            log.warn(">> 로그아웃 실패: 토큰 정보 없음");
        }
        
        // 쿠키 삭제
        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);
        
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
        
        return "redirect:/admin/login";
    }
    
    /**
     * 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, RedirectAttributes redirectAttributes) {
        log.error(">> 관리자 컨트롤러에서 예외 발생: {}", e.getMessage(), e);
        redirectAttributes.addFlashAttribute("errorMessage", "요청 처리 중 오류가 발생했습니다: " + e.getMessage());
        return "redirect:/admin/login";
    }
} 