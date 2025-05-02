package org.codeNbug.mainserver.domain.seat.service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 좌석 선택 동시성 제어에 필요한 Redis 분산 락 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class RedisLockService {
	private final RedisTemplate<String, String> redisTemplate;
	private static final String PREFIX = "seat:lock:";

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
	 * @return true: 락 취소 성공, false: 락 걸려있는 key value 없음
	 */
	public boolean unlock(String key, String value) {
		String storedValue = redisTemplate.opsForValue().get(key);
		if (value != null && value.equals(storedValue)) {
			redisTemplate.delete(key);
			return true;
		}
		return false;
	}

	/**
	 * Redis에서 주어진 키(lockKey)에 해당하는 락 값(lockValue)을 조회합니다.
	 *
	 * @param lockKey 조회할 Redis 락 키
	 * @return 해당 키에 저장된 락 값 (없으면 null)
	 */
	public String getLockValue(String lockKey) {
		return redisTemplate.opsForValue().get(lockKey);
	}

	public Long extractEventIdByUserId(Long userId) {
		Set<String> keys = redisTemplate.keys(PREFIX + userId + ":*");
		if (keys == null || keys.isEmpty()) {
			throw new IllegalStateException("선택된 좌석 정보가 존재하지 않습니다.");
		}
		String key = keys.iterator().next(); // e.g. seat:lock:26:1:22
		String[] parts = key.split(":");
		if (parts.length < 5) {
			throw new IllegalStateException("좌석 키 형식 오류: " + key);
		}
		return Long.parseLong(parts[3]);
	}

	public List<Long> getLockedSeatIdsByUserId(Long userId) {
		Set<String> keys = redisTemplate.keys(PREFIX + userId + ":*");
		if (keys == null || keys.isEmpty()) {
			throw new IllegalStateException("선택된 좌석이 없습니다.");
		}
		return keys.stream()
			.map(k -> {
				String[] parts = k.split(":");
				if (parts.length < 5)
					throw new IllegalStateException("좌석 키 형식 오류: " + k);
				return Long.parseLong(parts[4]);
			})
			.toList();
	}

	public void releaseAllLocks(Long userId) {
		Set<String> keys = redisTemplate.keys(PREFIX + userId + ":*");
		if (keys != null) {
			redisTemplate.delete(keys);
		}
	}
}