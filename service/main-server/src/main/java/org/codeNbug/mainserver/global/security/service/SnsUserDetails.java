package org.codeNbug.mainserver.global.security.service;

import lombok.Getter;
import org.codeNbug.mainserver.external.sns.Entity.SnsUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * SNS 로그인 사용자를 위한 UserDetails 구현체
 */
@Getter
public class SnsUserDetails implements UserDetails {

    private final SnsUser snsUser;
    private final String identifier; // socialId:provider 형식의 식별자

    public SnsUserDetails(SnsUser snsUser) {
        this.snsUser = snsUser;
        this.identifier = snsUser.getSocialId() + ":" + snsUser.getProvider();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 기본 권한은 "USER"로 설정
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        // SNS 로그인의 경우 비밀번호가 없으므로 null 반환
        return null;
    }

    @Override
    public String getUsername() {
        // 사용자 식별자로 socialId:provider 형식 사용
        return identifier;
    }

    public Long getUserId() {
        return snsUser.getId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
} 