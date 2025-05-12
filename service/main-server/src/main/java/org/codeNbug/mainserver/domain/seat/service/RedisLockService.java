package org.codeNbug.mainserver.domain.seat.service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 좌석 선택 및 예매 시 동시성 제어를 위한 Redis 분산 락 서비스
 * <p>
 * 사용자의 좌석 선택 상태를 Redis 키로 관리하여 중복 예매를 방지하고,
 * TTL을 활용해 일정 시간이 지나면 자동으로 락이 해제되도록 합니다.
 * <p>
 * Redis 키 형식: seat:lock:{userId}:{eventId}:{seatId}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockService {
	private final RedisTemplate<String, String> redisTemplate;
	private final RedisKeyScanner redisKeyScanner;
	private static final String PREFIX = "seat:lock:";
	public static final String ENTRY_TOKEN_STORAGE_KEY_NAME = "ENTRY_TOKEN";

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
	 * Redis에서 주어진 키(lockKey)에 해당하는 락 값(lockValue) 조회
	 *
	 * @param lockKey 조회할 Redis 락 키
	 * @return 해당 키에 저장된 락 값 (없으면 null)
	 */
	public String getLockValue(String lockKey) {
		return redisTemplate.opsForValue().get(lockKey);
	}

	/**
	 * Redis에 저장된 락 키 중 하나를 기준으로 사용자(userId)의 이벤트 ID 추출
	 *
	 * @param userId 사용자 ID
	 * @return 이벤트 ID
	 * @throws IllegalStateException 락이 없거나 키 형식이 잘못된 경우
	 */
	public Long extractEventIdByUserId(Long userId) {
		Set<String> keys = redisKeyScanner.scanKeys(PREFIX + userId + ":*:*");

		if (keys == null || keys.isEmpty()) {
			throw new IllegalStateException("[extractEventIdByUserId] 선택된 좌석 정보가 존재하지 않습니다.");
		}
		String key = keys.iterator().next(); // e.g. seat:lock:26:1:22
		String[] parts = key.split(":");
		if (parts.length < 5) {
			throw new IllegalStateException("좌석 키 형식 오류: " + key);
		}
		return Long.parseLong(parts[3]);
	}

	/**
	 * 사용자(userId)의 Redis 락 키들로부터 좌석 ID만 추출
	 *
	 * @param userId 사용자 ID
	 * @return 선택된 좌석 ID 목록
	 * @throws IllegalStateException 락이 없거나 키 형식이 잘못된 경우
	 */
	public List<Long> getLockedSeatIdsByUserId(Long userId) {
		Set<String> keys = redisKeyScanner.scanKeys(PREFIX + userId + ":*");
		if (keys == null || keys.isEmpty()) {
			throw new IllegalStateException("[getLockedSeatIdsByUserId] 선택된 좌석이 없습니다.");
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

	/**
	 * 해당 사용자(userId)의 모든 Redis 락 해제
	 *
	 * @param userId 사용자 ID
	 */
	public void releaseAllLocks(Long userId) {
		Set<String> keys = redisKeyScanner.scanKeys(PREFIX + userId + ":*");
		if (keys != null) {
			redisTemplate.delete(keys);
		}
	}

	/**
	 * entry Queue 의 대기열 해제
	 *
	 * @param userId 사용자 ID
	 */
	public void releaseAllEntryQueueLocks(Long userId) {
		Long deletedCount = redisTemplate.opsForHash().delete(ENTRY_TOKEN_STORAGE_KEY_NAME, userId.toString());

		if (deletedCount > 0) {
			log.info("ENTRY_TOKEN 해시에서 userId {}의 토큰을 삭제했습니다.", userId);
		} else {
			log.warn("ENTRY_TOKEN 해시에서 userId {}에 해당하는 토큰이 존재하지 않습니다.", userId);
		}
	}
}