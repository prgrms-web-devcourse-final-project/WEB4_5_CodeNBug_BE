package org.codeNbug.queueserver.external.redis;

import java.time.Duration;
import java.util.Map;

import org.codeNbug.queueserver.entryauth.service.EntryAuthService;
import org.codeNbug.queueserver.waitingqueue.entity.SseConnection;
import org.codeNbug.queueserver.waitingqueue.entity.Status;
import org.codeNbug.queueserver.waitingqueue.service.SseEmitterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EntryStreamMessageListener implements StreamListener<String, MapRecord<String, String, String>> {

	private final RedisTemplate<String, Object> redisTemplate;
	private final RedisConnectionFactory redisConnectionFactory;
	private final SseEmitterService sseEmitterService;
	private final EntryAuthService entryAuthService;

	@Value("${custom.instance-id}")
	private String instanceId;

	private StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;

	public EntryStreamMessageListener(RedisTemplate<String, Object> redisTemplate,
		RedisConnectionFactory redisConnectionFactory, SseEmitterService sseEmitterService,
		EntryAuthService entryAuthService) {
		this.redisTemplate = redisTemplate;
		this.redisConnectionFactory = redisConnectionFactory;
		this.sseEmitterService = sseEmitterService;
		this.entryAuthService = entryAuthService;
	}

	@PostConstruct
	public void startListening() {
		String groupName = RedisConfig.DISPATCH_QUEUE_CHANNEL_NAME + ":" + instanceId;
		String consumerName = instanceId + "-consumer"; // 각 인스턴스마다 고유한 컨슈머 이름

		// 컨슈머 그룹 생성 (이미 RedisConfig의 basicRedisTemplate 빈에서 시도함)
		try {
			// 스트림이 존재하지 않으면 BUSYGROUP 에러가 발생할 수 있으므로 확인
			if (redisTemplate.opsForStream().groups(RedisConfig.DISPATCH_QUEUE_CHANNEL_NAME).stream()
				.noneMatch(xInfoGroup -> xInfoGroup.groupName().equals(groupName))) {
				redisTemplate.opsForStream().createGroup(RedisConfig.DISPATCH_QUEUE_CHANNEL_NAME, groupName);
			}
		} catch (RedisSystemException e) {
			redisTemplate.opsForStream().createGroup(RedisConfig.DISPATCH_QUEUE_CHANNEL_NAME, groupName);
		}

		StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
			StreamMessageListenerContainer.StreamMessageListenerContainerOptions
				.builder()
				.pollTimeout(Duration.ofSeconds(1)) // 폴링 타임아웃 설정
				.batchSize(RedisConfig.ENTRY_QUEUE_CAPACITY) // 한 번에 가져올 메시지 수
				.build();

		streamMessageListenerContainer = StreamMessageListenerContainer.create(redisConnectionFactory, options);

		// '>'는 아직 처리되지 않은 새로운 메시지만 읽겠다는 의미
		// ReadOffset.lastConsumed()는 현재 컨슈머 그룹에서 마지막으로 처리(ack)한 메시지 다음부터 읽음
		streamMessageListenerContainer.receive(
			Consumer.from(groupName, consumerName),
			StreamOffset.create(RedisConfig.DISPATCH_QUEUE_CHANNEL_NAME, ReadOffset.lastConsumed()),
			this // 리스너로 현재 클래스 인스턴스 지정
		);

		streamMessageListenerContainer.start();
		log.info("Started listening to Redis Stream '{}' with consumer group '{}' and consumer name '{}'",
			RedisConfig.DISPATCH_QUEUE_CHANNEL_NAME, groupName, consumerName);
	}

	@PreDestroy
	public void stopListening() {
		if (streamMessageListenerContainer != null) {
			streamMessageListenerContainer.stop();
			log.info("Stopped listening to Redis Stream.");
		}
	}

	@Override
	public void onMessage(MapRecord<String, String, String> message) {

		String groupName = RedisConfig.DISPATCH_QUEUE_CHANNEL_NAME + ":" + instanceId;
		String consumerName = instanceId + "-consumer";

		Map<String, String> body = message.getValue();

		Long userId = Long.parseLong(body.get("userId"));
		Long eventId = Long.parseLong(body.get("eventId"));
		SseConnection sseConnection = sseEmitterService.getEmitterMap().get(userId);

		if (sseConnection == null || !sseConnection.getEventId().equals(eventId)) {
			return;
		}

		sseConnection.setStatus(Status.IN_PROGRESS);
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
			redisTemplate.opsForStream()
				.acknowledge(RedisConfig.DISPATCH_QUEUE_CHANNEL_NAME, groupName, message.getId());
		} catch (Exception e) {
			emitter.complete();
		}
	}
}
