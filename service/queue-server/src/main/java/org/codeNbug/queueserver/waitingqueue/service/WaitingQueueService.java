package org.codeNbug.queueserver.waitingqueue.service;

import org.codeNbug.mainserver.user.entity.User;
import org.codeNbug.mainserver.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class WaitingQueueService {

	private final UserService userService;
	private final SseEmitterService sseEmitterService;

	public WaitingQueueService(UserService userService, SseEmitterService sseEmitterService) {
		this.userService = userService;
		this.sseEmitterService = sseEmitterService;
	}

	public void entry(Long eventId) {
		// 로그인한 유저 조회
		User loggedInUser = userService.getLoggedInUser();

		// emitter 생성 및 저장
		sseEmitterService.add(loggedInUser.getId(), eventId);

		// TODO: waiting thread에 유저를 추가하도록 전달

	}
}
