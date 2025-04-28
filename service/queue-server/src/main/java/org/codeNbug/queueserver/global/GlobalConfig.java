package org.codeNbug.queueserver.global;

import org.codeNbug.mainserver.user.entity.User;
import org.codeNbug.mainserver.user.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlobalConfig {
	@Bean
	public UserService mockUserService() {
		// TODO: 실제 구현체가 만들어지면 이 부분을 구현체로 교체하세요.
		return new UserService() {
			@Override
			public User getLoggedInUser() {
				throw new UnsupportedOperationException("아직 구현되지 않았습니다.");

			}
		};
	}
}
