package org.codeNbug.mainserver.domain.seat.service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Redis에서 안전하게 키를 조회하기 위한 SCAN 기반 유틸
 */
@Component
@RequiredArgsConstructor
public class RedisKeyScanner {
	private final StringRedisTemplate redisTemplate;

	/**
	 * Redis에서 주어진 패턴에 매칭되는 키 목록 조회
	 *
	 * @param pattern 예: seat:lock:{userId}:{eventId}:*
	 * @return 매칭되는 키 목록
	 */
	public Set<String> scanKeys(String pattern) {
		Set<String> keys = new HashSet<>();
		ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();

		try (Cursor<byte[]> cursor = Objects.requireNonNull(redisTemplate.getConnectionFactory())
			.getConnection()
			.scan(options)) {
			while (cursor.hasNext()) {
				keys.add(new String(cursor.next()));
			}
		} catch (Exception e) {
			throw new RuntimeException("Redis SCAN 중 오류 발생: " + e.getMessage(), e);
		}
		return keys;
	}
}
