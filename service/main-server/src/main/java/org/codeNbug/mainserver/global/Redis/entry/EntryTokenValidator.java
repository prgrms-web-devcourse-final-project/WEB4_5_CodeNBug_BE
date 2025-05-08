package org.codeNbug.mainserver.global.Redis.entry;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EntryTokenValidator {

	private final StringRedisTemplate redisTemplate;

	public void validate(Long userId, String token) {
		String redisKey = String.format("ENTRY_TOKEN:%d:%s", userId, token);
		Boolean exists = redisTemplate.hasKey(redisKey);
		if (!exists) {
			throw new AccessDeniedException("유효하지 않은 입장 토큰입니다.");
		}
	}
}