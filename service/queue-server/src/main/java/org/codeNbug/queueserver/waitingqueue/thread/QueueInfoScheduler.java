package org.codeNbug.queueserver.waitingqueue.thread;

import static org.codeNbug.queueserver.external.redis.RedisConfig.*;

import java.util.List;
import java.util.Map;

import org.codeNbug.queueserver.waitingqueue.entity.SseConnection;
import org.codeNbug.queueserver.waitingqueue.service.SseEmitterService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class QueueInfoScheduler {

	private final RedisTemplate<String, String> redisTemplate;
	private final SseEmitterService emitterService;
	private final ObjectMapper objectMapper;

	public QueueInfoScheduler(RedisTemplate<String, String> redisTemplate, SseEmitterService emitterService,
		ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.emitterService = emitterService;
		this.objectMapper = objectMapper;
	}

	/**
	 * 대기열 순번 정보를 유저에게 전송하는 스레드 스케줄링 메서드
	 * 1초마다 수행됩니다.
	 */
	@Scheduled(cron = "* * * * * *")
	public void run() {

		// emitter map 가져옵니다
		Map<Long, SseConnection> emitterMap = emitterService.getEmitterMap();

		// redis waiting queue의 모든 요소를 가져옵니다
		redisTemplate.keys(WAITING_QUEUE_KEY_NAME + ":*").forEach(key -> {
			try {
				doPrintInfo(emitterMap, key);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		});

	}

	private void doPrintInfo(Map<Long, SseConnection> emitterMap, String key) throws JsonProcessingException {
		// eventId에 해당하는 모든 stream 가져오기
		List<Object> waitingList = redisTemplate.opsForZSet()
			.range(key, 0, -1).stream().map(
				item -> {
					try {
						return redisTemplate.opsForHash()
							.get("WAITING_QUEUE_RECORD:" + key.split(":")[1].toString(),
								objectMapper.readTree(item.toString()).get("userId").asText());
					} catch (JsonProcessingException e) {
						throw new RuntimeException(e);
					}
				}
			).toList();

		if (waitingList == null || waitingList.isEmpty()) {
			return;
		}
		// 대기열 큐에 있는 모든 유저들에게 대기열 순번과 userId, eventId를 전송합니다.
		for (Object record : waitingList) {
			// 대기열 큐 메시지로부터 데이터를 파싱합니다.
			Long userId = Long.parseLong(
				objectMapper.readTree(record.toString())
					.get(QUEUE_MESSAGE_USER_ID_KEY_NAME)
					.toString()
					.replaceAll("\"", ""));
			Long eventId = Long.parseLong(
				objectMapper.readTree(record.toString())
					.get(QUEUE_MESSAGE_EVENT_ID_KEY_NAME)
					.toString()
					.replaceAll("\"", ""));
			Long idx = Long.parseLong(
				objectMapper.readTree(record.toString())
					.get(QUEUE_MESSAGE_IDX_KEY_NAME)
					.toString()
					.replaceAll("\"", ""));

			if (!emitterMap.containsKey(userId)) {
				log.debug("user %d가 연결이 끊어진 상태입니다.".formatted(userId));
				continue;
			}

			// 파싱한 userId로 sse 연결 객체를 가져옵니다.
			SseConnection sseConnection = emitterMap.get(userId);
			SseEmitter emitter = sseConnection.getEmitter();
			// 대기열 순번을 계산하고 sse 메시지를 전송합니다.
			try {
				emitter.send(
					SseEmitter.event()
						.data(Map.of("status", sseConnection.getStatus(), QUEUE_MESSAGE_USER_ID_KEY_NAME, userId,
							QUEUE_MESSAGE_EVENT_ID_KEY_NAME, eventId, "order",
							redisTemplate.opsForZSet()
								.rank(WAITING_QUEUE_KEY_NAME + ":" + eventId,
									"{\"userId\":\"%s\"}".formatted(userId))
								+ 1))
				);
			} catch (Exception e) {
				emitter.complete();
				log.debug("user %d가 연결이 끊어진 상태입니다.".formatted(userId));
			}
		}
	}

	@Scheduled(cron = "*/5 * * * * *")
	public void heartBeat() {
		Map<Long, SseConnection> emitterMap = emitterService.getEmitterMap();
		for (SseConnection conn : emitterMap.values()) {
			SseEmitter emitter = conn.getEmitter();
			try {
				emitter.send(
					SseEmitter.event()
						.comment("heartBeat")
				);
			} catch (Exception e) {
				emitter.complete();
			}
		}
	}
}

