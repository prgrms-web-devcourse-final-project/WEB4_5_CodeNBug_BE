package org.codeNbug.queueserver.waitingqueue.controller;

import java.util.Map;

import org.codeNbug.queueserver.waitingqueue.service.SseEmitterService;
import org.codeNbug.queueserver.waitingqueue.service.WaitingQueueEntryService;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.security.annotation.RoleRequired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api/v1")
public class WaitingQueueController {

	private final WaitingQueueEntryService waitingQueueEntryService;
	private final SseEmitterService sseEmitterService;
	private final MeterRegistry registry;

	public WaitingQueueController(WaitingQueueEntryService waitingQueueEntryService,
		SseEmitterService sseEmitterService, MeterRegistry registry) {
		this.waitingQueueEntryService = waitingQueueEntryService;
		this.sseEmitterService = sseEmitterService;
		this.registry = registry;
	}

	@PostConstruct
	void bindGauge() {
		Gauge.builder("sse_connections_active", sseEmitterService.getEmitterMap(), Map::size)
			.description("Number of active SSE connections")
			.register(registry);
	}

	/**
	 * 로그인한 유저가 행사 id가 {@code event-id}인 행사의 티켓 대기열에 진입합니다.
	 * 대기열에 진입시 sse 연결을 진행합니다.
	 * @param eventId
	 */
	@RoleRequired({UserRole.USER})
	@GetMapping(value = "/events/{id}/tickets/waiting", produces = MediaType.TEXT_EVENT_STREAM_VALUE
		+ ";charset=UTF-8")
	public SseEmitter entryWaiting(@PathVariable("id") Long eventId) {
		return waitingQueueEntryService.entry(eventId);
	}
}
