package org.codeNbug.mainserver.global.security.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


/**
 * 커스텀 사용자 상세 정보 서비스
 * <p>
 * 스프링 시큐리티의 인증 메커니즘에서 사용자 정보를 로드하기 위한 서비스입니다.
 * 사용자 이름(username)을 기반으로 데이터베이스에서 사용자 정보를 조회하고
 * 스프링 시큐리티에서 사용할 수 있는 UserDetails 객체로 변환합니다.
 * </p>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * 사용자 이름으로 사용자 상세 정보를 로드합니다.

     * 주어진 사용자 이름(username)을 기반으로 데이터베이스에서 사용자 정보를 조회하고,
     * 스프링 시큐리티에서 사용할 수 있는 UserDetails 객체로 변환하여 반환
     * 
     * @param username 조회할 사용자의 이름(아이디)
     * @return 사용자 상세 정보를 포함한 UserDetails 객체
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우 발생하는 예외
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("username을 찾을 수 없습니다.: " + username);
    }
} 