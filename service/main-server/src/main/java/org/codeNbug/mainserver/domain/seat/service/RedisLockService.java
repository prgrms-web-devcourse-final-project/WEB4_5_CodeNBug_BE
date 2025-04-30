package org.codeNbug.mainserver.domain.seat.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 좌석 선택 동시성 제어에 필요한 Redis 분산 락 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class RedisLockService {
	private final StringRedisTemplate redisTemplate;

	/**
	 * Redis에 key가 존재하지 않을 경우 value를 설정하며 락 시도
	 *
	 * @param key     락을 설정할 Redis 키
	 * @param value   락 소유자 식별 값 (UUID 등)
	 * @param timeout 락의 유지 시간
	 * @return true: 락 획득 성공, false: 이미 다른 사용자가 락을 소유 중
	 */
	public boolean tryLock(String key, String value, Duration timeout) {
		return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout));
	}

	/**
	 * Redis에 저장된 값이 일치할 경우 해당 키를 삭제하여 락 해제
	 *
	 * @param key   해제할 Redis 키
	 * @param value 락 소유자 식별 값
	 */
	public void unlock(String key, String value) {
		String storedValue = redisTemplate.opsForValue().get(key);
		if (value != null && value.equals(storedValue)) {
			redisTemplate.delete(key);
		}
	}
}