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

	public SseEmitter add(Long userId) {
		// 새로운 emitter 생성
		SseEmitter emitter = new SseEmitter(0L);
		// emitter연결이 끊어질 때 만약 entry상태라면 entry count를 1 증가
		emitter.onCompletion(() -> {
			log.info("emitter completed");
			Status status = emitterMap.get(userId).getStatus();
			if (status.equals(Status.IN_ENTRY)) {
				redisTemplate.opsForValue()
					.increment(RedisConfig.ENTRY_QUEUE_COUNT_KEY_NAME, 1);
			} else if (status.equals(Status.IN_QUEUE)) {
				String recordIdString = redisTemplate.opsForHash()
					.get(RedisConfig.WAITING_QUEUE_IN_USER_RECORD_KEY_NAME, userId.toString()).toString();
				RecordId recordId = RecordId.of(recordIdString);

				redisTemplate.opsForStream()
					.delete(RedisConfig.WAITING_QUEUE_KEY_NAME, recordId);
				redisTemplate.opsForHash()
					.delete(RedisConfig.WAITING_QUEUE_IN_USER_RECORD_KEY_NAME, userId.toString());
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
		emitterMap.put(userId, new SseConnection(emitter, Status.IN_QUEUE));

		return emitter;
	}

}
