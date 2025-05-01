package org.codeNbug.queueserver.waitingqueue.service;

import org.codeNbug.queueserver.waitingqueue.thread.WaitingControllerThread;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class WaitingQueueService {

	private final SseEmitterService sseEmitterService;
	private final WaitingControllerThread waitingControllerThread;

	public WaitingQueueService(SseEmitterService sseEmitterService,
		WaitingControllerThread waitingControllerThread) {
		this.sseEmitterService = sseEmitterService;
		this.waitingControllerThread = waitingControllerThread;
	}

	public SseEmitter entry(Long eventId) {
		// 로그인한 유저 id 조회
		Long id = getLoggedInUserId();

		// emitter 생성 및 저장
		SseEmitter emitter = sseEmitterService.add(id);

		// TODO: waiting thread에 유저를 추가하도록 전달
		waitingControllerThread.enter(id, eventId);
		return emitter;
	}

	private Long getLoggedInUserId() {
		return 1L;
	}
}
