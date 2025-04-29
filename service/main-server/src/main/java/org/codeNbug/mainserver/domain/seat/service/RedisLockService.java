package org.codeNbug.mainserver.domain.seat.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisLockService {
	private final StringRedisTemplate redisTemplate;

	public boolean tryLock(String key, String value, Duration timeout) {
		return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout));
	}

	public void unlock(String key, String value) {
		String storedValue = redisTemplate.opsForValue().get(key);
		if (value.equals(storedValue)) {
			redisTemplate.delete(key);
		}
	}
}