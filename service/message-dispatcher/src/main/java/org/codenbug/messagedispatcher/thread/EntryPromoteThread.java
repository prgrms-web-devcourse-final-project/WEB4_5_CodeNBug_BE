package org.codenbug.messagedispatcher.thread;

import java.util.List;
import java.util.Map;

import org.codenbug.messagedispatcher.redis.RedisConfig;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
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
		StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();
		HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

		// 1) waiting 스트림의 모든 레코드를 조회
		List<MapRecord<String, Object, Object>> records =
			streamOps.range(RedisConfig.WAITING_QUEUE_KEY_NAME, Range.unbounded());

		for (MapRecord<String, Object, Object> record : records) {
			Long userId = Long.parseLong(record.getValue().get("userId").toString());
			Long eventId = Long.parseLong(record.getValue().get("eventId").toString());
			String instanceId = String.valueOf(record.getValue().get("instanceId"));

			// 이 스트림 메시지에 해당하는 유저가 이벤트의 entry queue에 들어갈수 있는지 검사
			// 해당 event의 entry queue count를 조회

			Long queueCount = Long.parseLong(
				hashOps.get(RedisConfig.ENTRY_QUEUE_COUNT_KEY_NAME, eventId.toString()).toString());
			if (queueCount != null && queueCount > 1) {
				// 2) 카운트 감소
				Long tmp = hashOps.increment(RedisConfig.ENTRY_QUEUE_COUNT_KEY_NAME, eventId.toString(), -1);
				System.out.println("tmp = " + tmp);
				System.out.println("eventId.toString() = " + eventId.toString());
				// entry queue message를 생성
				redisTemplate.opsForStream()
					.add(StreamRecords.mapBacked(
						Map.of("userId", userId, "eventId", eventId, "instanceId", instanceId)
					).withStreamKey(RedisConfig.ENTRY_QUEUE_KEY_NAME));
				// 3) 스트림 ACK
				streamOps.acknowledge(RedisConfig.WAITING_QUEUE_KEY_NAME, RedisConfig.WAITING_QUEUE_GROUP_NAME,
					record.getId());

				// 4) 스트림에서 해당 레코드 삭제
				streamOps.delete(RedisConfig.WAITING_QUEUE_KEY_NAME, record.getId());

				hashOps.delete(RedisConfig.WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + eventId.toString(),
					userId.toString());
			}
		}
		// // promote할 갯수 얻음
		//
		// Set<String> keys = redisTemplate.keys(RedisConfig.ENTRY_QUEUE_COUNT_KEY_NAME + ":*");
		//
		// Long count = Long.parseLong(Objects.requireNonNull(redisTemplate.opsForValue()
		// 	.get(RedisConfig.ENTRY_QUEUE_COUNT_KEY_NAME)).toString());
		// // 갯수만큼 waiting queue에서 가져옴
		// List<MapRecord<String, Object, Object>> promoteTarget = redisTemplate.opsForStream()
		// 	.read(Consumer.from(
		// 			RedisConfig.WAITING_QUEUE_GROUP_NAME, RedisConfig.WAITING_QUEUE_CONSUMER_NAME),
		// 		StreamReadOptions.empty().count(count), StreamOffset.create(
		// 			RedisConfig.WAITING_QUEUE_KEY_NAME, ReadOffset.lastConsumed()
		// 		));
		//
		// assert promoteTarget != null;
		// promoteTarget.forEach(
		// 	record -> {
		// 		// waiting queue message에서 userId, eventId, instanceId 추출
		// 		Long userId = Long.parseLong(record.getValue().get("userId").toString());
		// 		Long eventId = Long.parseLong(record.getValue().get("eventId").toString());
		// 		String instanceId = String.valueOf(record.getValue().get("instanceId"));
		//
		// 		// entry queue message를 생성
		// 		redisTemplate.opsForStream()
		// 			.add(StreamRecords.mapBacked(
		// 				Map.of("userId", userId, "eventId", eventId, "instanceId", instanceId)
		// 			).withStreamKey(RedisConfig.ENTRY_QUEUE_KEY_NAME));
		//
		// 		// waiting queue에서 메시지를 consume했음을 알림 (ack)
		// 		redisTemplate.opsForStream()
		// 			.acknowledge(RedisConfig.WAITING_QUEUE_KEY_NAME, RedisConfig.WAITING_QUEUE_GROUP_NAME,
		// 				record.getId());
		// 		// waiting queue에서 consume한 메시지를 삭제
		// 		redisTemplate.opsForStream()
		// 			.delete(RedisConfig.WAITING_QUEUE_KEY_NAME, record.getId());
		//
		// 		// WAITING_USER_ID에서 삭제
		// 		redisTemplate.opsForHash()
		// 			.delete(RedisConfig.WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + eventId, userId.toString());
		// 	}
		// );

	}
}
