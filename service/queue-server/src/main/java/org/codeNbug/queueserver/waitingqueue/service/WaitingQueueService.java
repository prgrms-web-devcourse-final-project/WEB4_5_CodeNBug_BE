package org.codeNbug.queueserver.waitingqueue.service;

import org.codeNbug.mainserver.user.entity.User;
import org.codeNbug.mainserver.user.service.UserService;
import org.codeNbug.queueserver.waitingqueue.thread.WaitingControllerThread;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class WaitingQueueService {

	private final UserService userService;
	private final SseEmitterService sseEmitterService;
	private final WaitingControllerThread waitingControllerThread;

	public WaitingQueueService(UserService userService, SseEmitterService sseEmitterService,
		WaitingControllerThread waitingControllerThread) {
		this.userService = userService;
		this.sseEmitterService = sseEmitterService;
		this.waitingControllerThread = waitingControllerThread;
	}

	public SseEmitter entry(Long eventId) {
		// 로그인한 유저 조회
		User loggedInUser = userService.getLoggedInUser();

		// emitter 생성 및 저장
		SseEmitter emitter = sseEmitterService.add(loggedInUser.getId());

		// TODO: waiting thread에 유저를 추가하도록 전달
		waitingControllerThread.enter(loggedInUser.getId(), eventId);
		return emitter;
	}
}
