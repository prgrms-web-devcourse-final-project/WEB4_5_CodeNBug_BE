package org.codeNbug.queueserver.waitingqueue.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseEmitterService {

	private static Map<Long, SseEmitter> emitterMap = new ConcurrentHashMap<Long, SseEmitter>();

	public SseEmitter add(Long userId, Long eventId) {
		// 새로운 emitter 생성
		SseEmitter emitter = new SseEmitter(0L);
		emitter.onCompletion(() -> emitterMap.remove(userId));

		// 초기 메시지 전달
		try {
			emitter.send(
				SseEmitter.event()
					.data("sse 연결 성공. userId:" + userId));
		} catch (Exception e) {
			emitter.complete();
		}

		// 전역 공간에 emitter 저장
		emitterMap.put(userId, emitter);

		return emitter;
	}

	public Map<Long, SseEmitter> getEmitterMap() {
		return emitterMap;
	}
}
