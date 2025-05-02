package org.codenbug.messagedispatcher.thread;

import java.util.Map;

import org.codenbug.messagedispatcher.redis.RedisConfig;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class EntryQueueConsumer {
	private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
	private final RedisTemplate<String, Object> redisTemplate;

	public EntryQueueConsumer(StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
		RedisTemplate<String, Object> redisTemplate) {
		this.container = container;
		this.redisTemplate = redisTemplate;
	}

	@PostConstruct
	public void startListening() {
		Subscription sub = container.receive(
			Consumer.from(RedisConfig.ENTRY_QUEUE_GROUP_NAME, RedisConfig.ENTRY_QUEUE_CONSUMER_NAME),
			StreamOffset.create(RedisConfig.ENTRY_QUEUE_KEY_NAME, ReadOffset.lastConsumed()),
			this::handleMessage);

	}

	private void handleMessage(MapRecord<String, String, String> record) {
		Map<String, String> body = record.getValue();
		// 1) 메시지 처리 로직
		redisTemplate.convertAndSend(RedisConfig.DISPATCH_QUEUE_CHANNEL_NAME, Map.of(
			"userId", body.get("userId"),
			"eventId", body.get("eventId"),
			"instanceId", body.get("instanceId")
		));
		// 2) ACK
		redisTemplate.opsForStream()
			.acknowledge(RedisConfig.ENTRY_QUEUE_KEY_NAME, RedisConfig.ENTRY_QUEUE_GROUP_NAME, record.getId());

	}

}

