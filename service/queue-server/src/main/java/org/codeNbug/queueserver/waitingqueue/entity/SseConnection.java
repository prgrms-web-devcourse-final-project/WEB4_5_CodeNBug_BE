package org.codeNbug.queueserver.waitingqueue.entity;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class SseConnection {

	private SseEmitter emitter;
	private Status status;

	public SseConnection() {
	}

	public SseConnection(SseEmitter emitter, Status status) {
		this.emitter = emitter;
		this.status = status;
	}

	public SseEmitter getEmitter() {
		return emitter;
	}

	public Status getStatus() {
		return status;
	}
}
