package org.codeNbug.queueserver.waitingqueue.entity;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.Setter;

public class SseConnection {

	private SseEmitter emitter;
	@Setter
	private Status status;

	private Long eventId;

	public SseConnection() {
	}

	public SseConnection(SseEmitter emitter, Status status, Long eventId) {
		this.emitter = emitter;
		this.status = status;
		this.eventId = eventId;
	}

	public SseEmitter getEmitter() {
		return emitter;
	}

	public Status getStatus() {
		return status;
	}

	public Long getEventId() {
		return eventId;
	}
}
