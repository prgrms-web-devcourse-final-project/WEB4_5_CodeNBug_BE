package org.codeNbug.queueserver.external.redis;

import java.io.IOException;
import java.util.Map;

import org.codeNbug.queueserver.entryauth.service.EntryAuthService;
import org.codeNbug.queueserver.waitingqueue.entity.SseConnection;
import org.codeNbug.queueserver.waitingqueue.entity.Status;
import org.codeNbug.queueserver.waitingqueue.service.SseEmitterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Subscriber implements MessageListener {

	private final ObjectMapper objectMapper;
	private final SseEmitterService sseEmitterService;
	private final RedisTemplate<String, Object> redisTemplate;
	private final EntryAuthService entryAuthService;

	@Value("${custom.instance-id}")
	private String instanceId;

	public Subscriber(ObjectMapper objectMapper, SseEmitterService sseEmitterService,
		RedisTemplate<String, Object> redisTemplate, EntryAuthService entryAuthService) {
		this.objectMapper = objectMapper;
		this.sseEmitterService = sseEmitterService;
		this.redisTemplate = redisTemplate;
		this.entryAuthService = entryAuthService;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		Map<String, Object> body = null;
		try {
			body = objectMapper.readValue(message.getBody(),
				new TypeReference<>() {
				});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		String parsedInstanceId = body.get("instanceId")
			.toString()
			.substring(1, body.get("instanceId").toString().length() - 1);
		if (instanceId.equals(parsedInstanceId)) {
			Long userId = Long.parseLong(body.get("userId").toString());
			Long eventId = Long.parseLong(body.get("eventId").toString());
			SseConnection sseConnection = sseEmitterService.getEmitterMap().get(userId);
			sseConnection.setStatus(Status.IN_ENTRY);
			SseEmitter emitter = sseConnection.getEmitter();

			String token = entryAuthService.generateEntryAuthToken(Map.of("eventId", eventId, "userId", userId),
				"entryAuthToken");
			redisTemplate.opsForHash()
				.put(RedisConfig.ENTRY_TOKEN_STORAGE_KEY_NAME, userId.toString(), token);
			try {

				emitter.send(
					SseEmitter.event()
						.data(Map.of(
							"eventId", eventId,
							"userId", userId,
							"status", sseConnection.getStatus(),
							"token", token
						))
				);
			} catch (Exception e) {
				emitter.complete();
			}
		}
	}
}
