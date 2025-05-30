package org.codeNbug.queueserver.waitingqueue.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codeNbug.queueserver.external.redis.RedisConfig;
import org.codeNbug.queueserver.waitingqueue.entity.SseConnection;
import org.codeNbug.queueserver.waitingqueue.entity.Status;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SseEmitterService {

	private static final Map<Long, SseConnection> emitterMap = new ConcurrentHashMap<>();

	public Map<Long, SseConnection> getEmitterMap() {
		return emitterMap;
	}

	private final RedisTemplate<String, Object> redisTemplate;

	public SseEmitterService(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public SseEmitter add(Long userId, Long eventId) {

		// 새로운 emitter 생성
		SseEmitter emitter = new SseEmitter(0L);
		// emitter연결이 끊어질 때 만약 entry상태라면 entry count를 1 증가
		emitter.onCompletion(() -> {
			log.info("emitter completed");
			// 커넥션 정보 얻기
			SseConnection sseConnection = emitterMap.get(userId);
			Status status = sseConnection.getStatus();
			// 커넥션 정보로부터 이벤트 아이디 얻기
			String parsedEventId = sseConnection.getEventId().toString();
			// 대기열 탈출 상태에서 커넥션이 종료되었다면
			// entry_queue_count를 1 감소시킨 것을 다시 증가
			if (status.equals(Status.IN_ENTRY)) {
				redisTemplate.opsForHash()
					.increment(RedisConfig.ENTRY_QUEUE_COUNT_KEY_NAME, parsedEventId, 1);
			} else if (status.equals(Status.IN_QUEUE)) {
				String recordIdString = redisTemplate.opsForHash()
					.get(RedisConfig.WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + parsedEventId,
						userId.toString())
					.toString();
				RecordId recordId = RecordId.of(recordIdString);

				redisTemplate.opsForStream()
					.delete(RedisConfig.WAITING_QUEUE_KEY_NAME + ":" + eventId, recordId);
				redisTemplate.opsForHash()
					.delete(RedisConfig.WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + parsedEventId,
						userId.toString());
			}
			emitterMap.remove(userId);
		});
		emitter.onError((e) -> {
			log.info("emitter error");
		});
		emitter.onTimeout(() -> {
			log.info("emitter timeout");
		});

		// 초기 메시지 전달
		try {
			emitter.send(
				SseEmitter.event()
					.data("sse 연결 성공. userId:" + userId));
		} catch (Exception e) {
			emitter.complete();
		}

		// 전역 공간에 emitter 저장
		emitterMap.put(userId, new SseConnection(emitter, Status.IN_QUEUE, eventId));

		return emitter;
	}
}
