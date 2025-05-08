package org.codeNbug.mainserver.domain.event.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.repository.JpaCommonEventRepository;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;

@Component
public class EventViewCountUpdateScheduler {

	private final RedisTemplate<String, Object> redisTemplate;

	private final JpaCommonEventRepository repository;

	public EventViewCountUpdateScheduler(RedisTemplate<String, Object> redisTemplate,
		JpaCommonEventRepository repository) {
		this.redisTemplate = redisTemplate;
		this.repository = repository;
	}

	@Scheduled(cron = "0 0 0 * * *")
	@Transactional
	public void updateViewCount() {
		List<Long> eventIds = redisTemplate.execute((RedisConnection connection) -> {
			ScanOptions opts = ScanOptions.scanOptions()
				.match("viewCount:*")
				.count(100)      // 한 번에 스캔할 개수(운영 부하 고려해 조절)
				.build();

			Cursor<byte[]> cursor = connection.scan(opts);
			Set<String> results = new HashSet<>();
			while (cursor.hasNext()) {
				results.add(new String(cursor.next(), StandardCharsets.UTF_8));
			}
			return results;
		}).stream().map(key ->
			Long.parseLong(key.substring("viewCount:".length()))
		).toList();

		Map<Long, Event> eventMap = new HashMap<>();
		repository.findAllById(eventIds).forEach(event -> eventMap.put(event.getEventId(), event));

		eventIds.forEach(id -> {
			String key = "viewCount:" + id;
			Object value = redisTemplate.opsForValue().get(key);
			if (value != null && eventMap.containsKey(id)) {
				Event event = eventMap.get(id);
				event.setViewCount(event.getViewCount() + Integer.parseInt(value.toString()));
				redisTemplate.delete(key);
			}
		});

	}
}
