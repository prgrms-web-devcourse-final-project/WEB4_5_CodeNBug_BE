package org.codeNbug.queueserver.waitingqueue.controller;

import java.util.Map;

import org.codeNbug.queueserver.waitingqueue.service.WaitingQueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
	@PostMapping("/events/{event-id}/tickets/waiting")
	public ResponseEntity<Map<String, Object>> entryWaiting(@PathVariable("event-id") Long eventId) {
		waitingQueueService.entry(eventId);

		return ResponseEntity.ok()
			.body(Map.of("code", 200, "msg", "대기열 진입 성공.", "data", null));
	}
}
