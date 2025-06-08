package org.codeNbug.mainserver.domain.event.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.repository.JpaCommonEventRepository;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;

@Component
public class EventViewCountUpdateScheduler {

	private final RedisTemplate<String, Object> redisTemplate;

	private final JpaCommonEventRepository repository;
	private static final Pattern EVENT = Pattern.compile("^event:(\\d+)$");

	public EventViewCountUpdateScheduler(RedisTemplate<String, Object> redisTemplate,
		JpaCommonEventRepository repository) {
		this.redisTemplate = redisTemplate;
		this.repository = repository;
	}

	@Scheduled(cron = "0 0 0 * * *")
	@Transactional
	public void updateViewCount() {

		Map<Long, Long> result = new LinkedHashMap<>();   // 순서 유지(1등→하위)

		ScanOptions opts = ScanOptions.scanOptions()
			.match("event:*")   // 멤버 패턴 필터(선택)
			.count(1_000)       // 배치 크기
			.build();

		try (Cursor<ZSetOperations.TypedTuple<Object>> cur =
				 redisTemplate.opsForZSet().scan("viewCount:top", opts)) {

			cur.forEachRemaining(tuple -> {
				String member = tuple.getValue().toString();
				Double score = tuple.getScore();      // null 아님

				Matcher m = EVENT.matcher(member);
				if (m.matches()) {
					long eventId = Long.parseLong(m.group(1));
					long viewCount = score.longValue();
					result.put(eventId, viewCount);
				}
			});
		}

		Map<Long, Event> eventMap = new HashMap<>();
		repository.findAllById(eventMap.keySet())
			.forEach(event -> event.setViewCount(eventMap.get(event.getEventId()).getViewCount()));
	}
}
