package org.codenbug.user.global.security.service;

import org.codenbug.user.user.entity.User;
import org.codenbug.user.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 커스텀 사용자 상세 정보 서비스
 * 스프링 시큐리티의 인증 메커니즘에서 사용자 정보를 로드하기 위한 서비스입니다.
 * 사용자 이름(username)을 기반으로 데이터베이스에서 사용자 정보를 조회하고
 * 스프링 시큐리티에서 사용할 수 있는 UserDetails 객체로 변환합니다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	/**
	 * 사용자 이름(이메일)으로 사용자 상세 정보를 로드합니다.
	 *
	 * 주어진 사용자 이름(이메일)을 기반으로 데이터베이스에서 사용자 정보를 조회하고,
	 * 스프링 시큐리티에서 사용할 수 있는 UserDetails 객체로 변환하여 반환
	 *
	 * @param username 조회할 사용자의 이름(이메일)
	 * @return 사용자 상세 정보를 포함한 UserDetails 객체
	 * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우 발생하는 예외
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(username)
			.orElseThrow(() -> new UsernameNotFoundException("이메일을 찾을 수 없습니다.: " + username));

		return new CustomUserDetails(user);
	}
}