package org.codeNbug.mainserver.global.util;

import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.security.exception.AuthenticationFailedException;
import org.codenbug.user.security.service.CustomUserDetails;
import org.codenbug.user.security.service.SnsUserDetails;
import org.codenbug.user.sns.Entity.SnsUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 보안 관련 유틸리티 클래스
 * 현재 인증된 사용자의 정보를 쉽게 가져올 수 있는 메서드들을 제공합니다.
 * 일반 사용자와 SNS 사용자 모두 지원합니다.
 */
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityUtil {

    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     * 일반 사용자와 SNS 사용자 모두 지원합니다.
     *
     * @return 현재 인증된 사용자의 ID
     * @throws AuthenticationFailedException 인증된 사용자가 없거나 인증 정보를 찾을 수 없는 경우
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationFailedException("인증된 사용자를 찾을 수 없습니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUserId();
        } else if (principal instanceof SnsUserDetails) {
            return ((SnsUserDetails) principal).getUserId();
        } else {
            throw new AuthenticationFailedException("지원되지 않는 사용자 유형입니다.");
        }
    }

    /**
     * 현재 인증된 사용자가 일반 사용자인지 SNS 사용자인지 확인합니다.
     *
     * @return 일반 사용자면 true, SNS 사용자면 false
     * @throws AuthenticationFailedException 인증된 사용자가 없거나 인증 정보를 찾을 수 없는 경우
     */
    public static boolean isRegularUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationFailedException("인증된 사용자를 찾을 수 없습니다.");
        }

        return authentication.getPrincipal() instanceof CustomUserDetails;
    }

    /**
     * 현재 인증된 사용자의 식별자를 반환합니다.
     * 일반 사용자의 경우 이메일, SNS 사용자의 경우 socialId:provider 형식입니다.
     *
     * @return 현재 인증된 사용자의 식별자
     * @throws AuthenticationFailedException 인증된 사용자가 없거나 인증 정보를 찾을 수 없는 경우
     */
    public static String getCurrentUserIdentifier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationFailedException("인증된 사용자를 찾을 수 없습니다.");
        }

        return authentication.getName();
    }

    /**
     * 현재 인증된 사용자의 이메일을 반환합니다.
     * SNS 사용자의 경우 저장된 이메일을 반환하거나 없으면 null을 반환합니다.
     *
     * @return 현재 인증된 사용자의 이메일
     * @throws AuthenticationFailedException 인증된 사용자가 없거나 인증 정보를 찾을 수 없는 경우
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationFailedException("인증된 사용자를 찾을 수 없습니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUsername();
        } else if (principal instanceof SnsUserDetails) {
            return ((SnsUserDetails) principal).getSnsUser().getEmail();
        } else {
            throw new AuthenticationFailedException("지원되지 않는 사용자 유형입니다.");
        }
    }

    /**
     * 현재 인증된 사용자의 User 엔티티를 반환합니다.
     * SNS 사용자의 경우 예외가 발생합니다.
     *
     * @return 현재 인증된 사용자의 User 엔티티
     * @throws AuthenticationFailedException 인증된 사용자가 없거나 인증 정보를 찾을 수 없는 경우, 또는 SNS 사용자인 경우
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AuthenticationFailedException("일반 사용자를 찾을 수 없습니다.");
        }

        return ((CustomUserDetails) authentication.getPrincipal()).getUser();
    }

    /**
     * 현재 인증된 SNS 사용자의 SnsUser 엔티티를 반환합니다.
     * 일반 사용자의 경우 예외가 발생합니다.
     *
     * @return 현재 인증된 사용자의 SnsUser 엔티티
     * @throws AuthenticationFailedException 인증된 사용자가 없거나 인증 정보를 찾을 수 없는 경우, 또는 일반 사용자인 경우
     */
    public static SnsUser getCurrentSnsUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            !(authentication.getPrincipal() instanceof SnsUserDetails)) {
            throw new AuthenticationFailedException("SNS 사용자를 찾을 수 없습니다.");
        }

        return ((SnsUserDetails) authentication.getPrincipal()).getSnsUser();
    }
}