package org.codenbug.messagedispatcher.thread;

import static org.codenbug.messagedispatcher.redis.RedisConfig.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EntryPromoteThread {

	private final RedisTemplate<String, Object> redisTemplate;
	private final ObjectMapper objectMapper;
	private final DefaultRedisScript<Long> promoteAllScript;

	public EntryPromoteThread(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		promoteAllScript = new DefaultRedisScript<>();
		promoteAllScript.setScriptText(loadLuaScriptFromResource("promote_all_waiting_for_event.lua"));
		promoteAllScript.setResultType(Long.class);
	}

	private String loadLuaScriptFromResource(String scriptName) {
		try (InputStream is =
				 new ClassPathResource(scriptName).getInputStream();
			 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			return reader.lines().collect(Collectors.joining("\n"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Scheduled(cron = "* * * * * *")
	public void promoteToEntryQueue() {
		// 1) waiting 스트림의 모든 레코드를 조회
		redisTemplate.multi();
		try {
			List<String> keys = redisTemplate.keys(WAITING_QUEUE_KEY_NAME + ":*").stream().collect(Collectors.toList());
			for (String key : keys) {
				String eventId = key.split(":")[1];
				String entryCountHashKey = ENTRY_QUEUE_COUNT_KEY_NAME;                    // ex: "ENTRY_QUEUE_COUNT"
				String waitingRecordHash =
					"WAITING_QUEUE_RECORD:" + eventId;            // ex: "WAITING_QUEUE_RECORD:42"
				String waitingZsetKey = WAITING_QUEUE_KEY_NAME + ":" + eventId;       // ex: "waiting:42"
				String waitingInUserHash =
					WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + eventId; // ex: "WAITING_QUEUE_IN_USER_RECORD:42"
				String entryStreamKey = ENTRY_QUEUE_KEY_NAME;                         // ex: "ENTRY_QUEUE"

				List<String> scriptKeys = List.of(
					entryCountHashKey,
					waitingRecordHash,
					waitingZsetKey,
					waitingInUserHash,
					entryStreamKey
				);

				// ARGV는 [eventId] 하나만 필요
				Long result = redisTemplate.execute(
					promoteAllScript,
					scriptKeys,
					Long.parseLong(eventId)
				);
				if (result == null) {
					throw new RuntimeException("promoteToEntryQueue failed");
				}
				if (result == 0L) {
					throw new RuntimeException("promoteToEntryQueue failed");
				}
			}
		} catch (Exception e) {
			log.info(e.getMessage());
			redisTemplate.discard();
		}
		redisTemplate.exec();
	}

	private void doPromote(String key) throws JsonProcessingException {

		HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

		// 해당 키의 스트림 내의 모든 메시지 얻기
		List<Object> records =
			redisTemplate.opsForZSet()
				.range(key, 0, -1).stream().map(
					item -> {
						try {
							return redisTemplate.opsForHash()
								.get("WAITING_QUEUE_RECORD:" + key.split(":")[1].toString(),
									objectMapper.readTree(item.toString()).get("userId").toString());
						} catch (JsonProcessingException e) {
							throw new RuntimeException(e);
						}
					}
				).toList();

		for (Object record : records) {
			// 메시지 내 데이터 파싱
			Long userId = Long.parseLong(objectMapper.readTree(record.toString()).get("userId").toString());
			Long eventId = Long.parseLong(objectMapper.readTree(record.toString()).get("eventId").toString());
			String instanceId = String.valueOf(objectMapper.readTree(record.toString()).get("instanceId"));

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

				// 4) 스트림에서 해당 레코드 삭제
				redisTemplate.opsForZSet().remove(key,
					objectMapper.writeValueAsString(Map.of("userId", userId)));
				redisTemplate.opsForHash().delete("WAITING_QUEUE_RECORD:" + eventId.toString(), userId.toString());

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
