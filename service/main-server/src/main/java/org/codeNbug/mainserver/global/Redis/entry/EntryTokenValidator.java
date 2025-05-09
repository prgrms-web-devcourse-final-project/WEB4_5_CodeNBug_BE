package org.codeNbug.mainserver.global.Redis.entry;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntryTokenValidator {

	private final StringRedisTemplate redisTemplate;
	public static final String ENTRY_TOKEN_STORAGE_KEY_NAME = "ENTRY_TOKEN";

	public void validate(Long userId, String token) {
		String redisKey = ENTRY_TOKEN_STORAGE_KEY_NAME;
		String storedToken = (String)redisTemplate.opsForHash().get(redisKey, userId.toString());

		if (storedToken == null) {
			throw new AccessDeniedException("유효하지 않은 입장 토큰입니다.");
		}

		storedToken = storedToken.replace("\"", "");  // 쌍따옴표 제거

		if (!storedToken.equals(token)) {
			throw new AccessDeniedException("유효하지 않은 입장 토큰입니다.");
		}
	}
}