package org.codeNbug.queueserver.waitingqueue.controller;

import org.codeNbug.queueserver.waitingqueue.service.WaitingQueueService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
public class WaitingQueueController {

	private final WaitingQueueService waitingQueueService;

	public WaitingQueueController(WaitingQueueService waitingQueueService) {
		this.waitingQueueService = waitingQueueService;
	}

	/**
	 * 로그인한 유저가 행사 id가 {@code event-id}인 행사의 티켓 대기열에 진입합니다.
	 * 대기열에 진입시 sse 연결을 진행합니다.
	 * @param eventId
	 */
	@GetMapping(value = "/events/{event-id}/tickets/waiting", produces = MediaType.TEXT_EVENT_STREAM_VALUE
		+ ";charset=UTF-8")
	public SseEmitter entryWaiting(@PathVariable("event-id") Long eventId) {
		return waitingQueueService.entry(eventId);
	}
}
