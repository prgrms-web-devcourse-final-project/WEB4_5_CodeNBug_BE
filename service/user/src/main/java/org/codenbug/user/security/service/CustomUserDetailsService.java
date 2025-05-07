package org.codenbug.user.security.service;

import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.sns.Entity.SnsUser;
import org.codenbug.user.sns.repository.SnsUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 커스텀 사용자 상세 정보 서비스
 * 스프링 시큐리티의 인증 메커니즘에서 사용자 정보를 로드하기 위한 서비스입니다.
 * 일반 사용자와 SNS 로그인 사용자 모두 지원합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;
	private final SnsUserRepository snsUserRepository;

	/**
	 * 사용자 식별자로 사용자 상세 정보를 로드합니다.
	 *
	 * @param identifier 사용자 식별자 (일반 사용자의 경우 이메일, SNS 사용자의 경우 socialId:provider 형식)
	 * @return 사용자 상세 정보를 포함한 UserDetails 객체
	 * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우 발생하는 예외
	 */
	@Override
	public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
		log.debug(">> 사용자 식별자로 정보 로드: {}", identifier);

		// SNS 사용자 식별자 형식 (socialId:provider)인 경우
		if (identifier.contains(":")) {
			log.debug(">> SNS 사용자 식별자 감지: {}", identifier);
			String[] parts = identifier.split(":");
			String socialId = parts[0];

			SnsUser snsUser = snsUserRepository.findBySocialId(socialId)
				.orElseThrow(() -> {
					log.warn(">> SNS 사용자를 찾을 수 없음: socialId={}", socialId);
					return new UsernameNotFoundException("SNS 사용자를 찾을 수 없습니다: " + socialId);
				});

			log.debug(">> SNS 사용자 찾음: socialId={}, provider={}", snsUser.getSocialId(), snsUser.getProvider());
			return new SnsUserDetails(snsUser);
		}
		// 일반 사용자 (이메일)인 경우
		else {
			log.debug(">> 일반 사용자 이메일 감지: {}", identifier);
			User user = userRepository.findByEmail(identifier)
				.orElseThrow(() -> {
					log.warn(">> 일반 사용자를 찾을 수 없음: email={}", identifier);
					return new UsernameNotFoundException("이메일을 찾을 수 없습니다: " + identifier);
				});

			log.debug(">> 일반 사용자 찾음: email={}, userId={}", user.getEmail(), user.getUserId());
			return new CustomUserDetails(user);
		}
	}
} 