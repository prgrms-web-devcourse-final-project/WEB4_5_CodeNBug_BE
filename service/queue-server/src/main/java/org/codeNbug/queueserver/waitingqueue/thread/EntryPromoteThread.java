package org.codeNbug.queueserver.waitingqueue.thread;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.codeNbug.queueserver.external.redis.RedisConfig;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EntryPromoteThread {

	private final RedisTemplate<String, Object> redisTemplate;

	public EntryPromoteThread(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Scheduled(cron = "* * * * * *")
	public void promoteToEntryQueue() {
		// promote할 갯수 얻음
		Long count = Long.parseLong(Objects.requireNonNull(redisTemplate.opsForValue()
			.get(RedisConfig.ENTRY_QUEUE_COUNT_KEY_NAME)).toString());

		// 갯수만큼 waiting queue에서 가져옴
		List<MapRecord<String, Object, Object>> promoteTarget = redisTemplate.opsForStream()
			.read(Consumer.from(
					RedisConfig.WAITING_QUEUE_GROUP_NAME, RedisConfig.WAITING_QUEUE_CONSUMER_NAME),
				StreamReadOptions.empty().count(count), StreamOffset.create(
					RedisConfig.WAITING_QUEUE_KEY_NAME, ReadOffset.lastConsumed()
				));

		assert promoteTarget != null;
		promoteTarget.forEach(
			record -> {
				// waiting queue message에서 userId, eventId, instanceId 추출
				Long userId = Long.parseLong(record.getValue().get("userId").toString());
				Long eventId = Long.parseLong(record.getValue().get("eventId").toString());
				String instanceId = record.getValue().get("instanceId").toString();

				// entry queue message를 생성
				redisTemplate.opsForStream()
					.add(StreamRecords.mapBacked(
						Map.of("userId", userId, "eventId", eventId, "instanceId", instanceId)
					).withStreamKey(RedisConfig.ENTRY_QUEUE_KEY_NAME));

				// waiting queue에서 메시지를 consume했음을 알림 (ack)
				redisTemplate.opsForStream()
					.acknowledge(RedisConfig.WAITING_QUEUE_KEY_NAME, RedisConfig.WAITING_QUEUE_GROUP_NAME,
						record.getId());
				// waiting queue에서 consume한 메시지를 삭제
				redisTemplate.opsForStream()
					.delete(RedisConfig.WAITING_QUEUE_KEY_NAME, record.getId());
				redisTemplate.opsForHash()
					.delete(RedisConfig.WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + eventId, userId.toString());
			}
		);
		// entry queue의 최대 크기에서 waiting queue에서 가져온 갯수만큼을 뺀 갯수를 저장.
		// 다음번에 가져올 최대 갯수를 계산해 저장
		if (!promoteTarget.isEmpty()) {
			redisTemplate.opsForValue()
				.set(RedisConfig.ENTRY_QUEUE_COUNT_KEY_NAME, RedisConfig.ENTRY_QUEUE_CAPACITY - promoteTarget.size());
		}
	}
}
