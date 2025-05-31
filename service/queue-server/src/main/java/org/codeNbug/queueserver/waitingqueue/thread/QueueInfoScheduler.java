package org.codeNbug.queueserver.waitingqueue.thread;

import static org.codeNbug.queueserver.external.redis.RedisConfig.*;

import java.util.List;
import java.util.Map;

import org.codeNbug.queueserver.waitingqueue.entity.SseConnection;
import org.codeNbug.queueserver.waitingqueue.service.SseEmitterService;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class QueueInfoScheduler {

	private final RedisTemplate<String, Object> redisTemplate;
	private final SseEmitterService emitterService;

	public QueueInfoScheduler(RedisTemplate<String, Object> redisTemplate, SseEmitterService emitterService) {
		this.redisTemplate = redisTemplate;
		this.emitterService = emitterService;

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
			doPrintInfo(emitterMap, key);
		});

	}

	private void doPrintInfo(Map<Long, SseConnection> emitterMap, String key) {
		StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();
		// eventId에 해당하는 모든 stream 가져오기
		List<MapRecord<String, Object, Object>> waitingList = streamOps.range(key,
			Range.unbounded());

		if (waitingList == null || waitingList.isEmpty()) {
			return;
		}

		Long firstIdx = Long.parseLong(
			waitingList.getFirst().getValue().get(QUEUE_MESSAGE_IDX_KEY_NAME).toString());

		// 대기열 큐에 있는 모든 유저들에게 대기열 순번과 userId, eventId를 전송합니다.
		for (MapRecord<String, Object, Object> record : waitingList) {
			// 대기열 큐 메시지로부터 데이터를 파싱합니다.
			Long userId = Long.parseLong(record.getValue().get(QUEUE_MESSAGE_USER_ID_KEY_NAME).toString());
			Long eventId = Long.parseLong(record.getValue().get(QUEUE_MESSAGE_EVENT_ID_KEY_NAME).toString());
			Long idx = Long.parseLong(record.getValue().get(QUEUE_MESSAGE_IDX_KEY_NAME).toString());

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
							QUEUE_MESSAGE_EVENT_ID_KEY_NAME, eventId, "order", idx - firstIdx + 1))
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

