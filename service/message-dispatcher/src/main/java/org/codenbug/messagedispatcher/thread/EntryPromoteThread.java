package org.codenbug.messagedispatcher.thread;

import static org.codenbug.messagedispatcher.redis.RedisConfig.*;

import java.util.List;
import java.util.Map;

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
		// 1) waiting 스트림의 모든 레코드를 조회
		redisTemplate.keys(WAITING_QUEUE_KEY_NAME + ":*").forEach(key -> {
			doPromote(key);
		});
	}

	private void doPromote(String key) {
		// waiting stream에 consumer group이 없다면 만들어 줘야 함.
		try {
			if (redisTemplate.opsForStream()
				.groups(key)
				.stream()
				.noneMatch(xInfoGroup -> xInfoGroup.groupName().equals(WAITING_QUEUE_GROUP_NAME))) {
				redisTemplate.opsForStream().createGroup(key, WAITING_QUEUE_GROUP_NAME);
			}

		} catch (Exception e) {
			redisTemplate.opsForStream().createGroup(key, WAITING_QUEUE_GROUP_NAME);
		}

		StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();
		HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

		// 해당 키의 스트림 내의 모든 메시지 얻기
		List<MapRecord<String, Object, Object>> records =
			streamOps.range(key, Range.unbounded());

		for (MapRecord<String, Object, Object> record : records) {
			// 메시지 내 데이터 파싱
			Long userId = Long.parseLong(record.getValue().get("userId").toString());
			Long eventId = Long.parseLong(record.getValue().get("eventId").toString());
			String instanceId = String.valueOf(record.getValue().get("instanceId"));

			// 이 waiting 스트림 메시지에 해당하는 유저가 이벤트의 entry queue에 들어갈수 있는지 검사
			// 해당 event의 entry queue count를 조회
			Long queueCount = Long.parseLong(
				hashOps.get(ENTRY_QUEUE_COUNT_KEY_NAME, eventId.toString()).toString());
			// 이 값이 1 이상이라면 들어갈 자리가 있다는 뜻이므로 유저를 entry queue로 넣음
			if (queueCount != null && queueCount > 0) {

				// 2) 해당 event의 entry queue count 1만큼 감소
				Long tmp = hashOps.increment(ENTRY_QUEUE_COUNT_KEY_NAME, eventId.toString(), -1);

				// entry queue message를 생성
				redisTemplate.opsForStream()
					.add(StreamRecords.mapBacked(
						Map.of("userId", userId, "eventId", eventId, "instanceId", instanceId)
					).withStreamKey(ENTRY_QUEUE_KEY_NAME));

				// 3) 스트림 ACK
				streamOps.acknowledge(key, WAITING_QUEUE_GROUP_NAME,
					record.getId());

				// 4) 스트림에서 해당 레코드 삭제
				streamOps.delete(key, record.getId());

				hashOps.delete(WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + eventId.toString(),
					userId.toString());
			}
		}
	}
}

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
