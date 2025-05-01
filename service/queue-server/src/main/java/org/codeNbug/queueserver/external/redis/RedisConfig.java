package org.codeNbug.queueserver.external.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	// waiting queue 컨슈머 생성 시 그룹 이름
	public static final String WAITING_QUEUE_GROUP_NAME = "WAITING_QUEUE";
	// waiting queue redis stream 키 이름
	public static final String WAITING_QUEUE_KEY_NAME = "WAITING";
	public static final Long ENTRY_QUEUE_CAPACITY = 1000L;
	// 메시지의 idx 값을 저장하기 위한 space의 key값
	public static final String WAITING_QUEUE_IDX_KEY_NAME = "WAITING_QUEUE_IDX";
	// entry queue의 현재 인원을  저장하기 위한 space의 key값
	public static final String ENTRY_QUEUE_COUNT_KEY_NAME = "ENTRY_QUEUE_COUNT";
	// 메시지 내부의 userId 속성의 키 값
	public static final String QUEUE_MESSAGE_USER_ID_KEY_NAME = "userId";
	// 메시지 내부의 eventId 속성의 키 값
	public static final String QUEUE_MESSAGE_EVENT_ID_KEY_NAME = "eventId";
	public static final String QUEUE_MESSAGE_INSTANCE_ID_KEY_NAME = "instanceId";
	// 메시지 내부의 idx 속성의 키 값
	public static final String QUEUE_MESSAGE_IDX_KEY_NAME = "idx";
	private static final String DISPATCH_QUEUE_CHANNEL_NAME = "DISPATCH";
	public static final String WAITING_QUEUE_IN_USER_RECORD_KEY_NAME = "WAITING_USER_ID";

	@Value("${custom.instance-id}")
	private String instanceId;

	@Bean
	@Qualifier("pubSubConnectionFactory")
	public LettuceConnectionFactory redisPubSubConnectionFactory(
		@Value("${spring.data.redis.host}") String host, @Value("${spring.data.redis.port}") int port
	) {

		LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
		factory.afterPropertiesSet();
		return factory;
	}

	@Bean
	public RedisTemplate<String, Object> basicRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		StringRedisSerializer keySerializer = new StringRedisSerializer();
		// 메시지 필드와 값은 JSON
		GenericJackson2JsonRedisSerializer jsonSerializer =
			new GenericJackson2JsonRedisSerializer();

		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setKeySerializer(keySerializer);
		redisTemplate.setHashKeySerializer(keySerializer);
		redisTemplate.setHashValueSerializer(jsonSerializer);
		redisTemplate.setValueSerializer(jsonSerializer);
		redisTemplate.afterPropertiesSet();

		// 컨슈머 그룹이 없으면 새로운 컨슈머 그룹 생성
		try {
			if (!redisTemplate.opsForStream()
				.groups(WAITING_QUEUE_KEY_NAME).stream().anyMatch(
					xInfoGroup -> xInfoGroup.groupName().equals(WAITING_QUEUE_GROUP_NAME + ":" + instanceId)
				)) {
				redisTemplate.opsForStream()
					.createGroup(WAITING_QUEUE_KEY_NAME, WAITING_QUEUE_GROUP_NAME + ":" + instanceId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			redisTemplate.opsForStream()
				.createGroup(WAITING_QUEUE_KEY_NAME, WAITING_QUEUE_GROUP_NAME + ":" + instanceId);
		}
		// 메시지 idx 데이터가 없다면 생성.
		if (redisTemplate.opsForValue().get(WAITING_QUEUE_IDX_KEY_NAME) == null) {
			redisTemplate.opsForValue()
				.set(WAITING_QUEUE_IDX_KEY_NAME, 0L);
		}
		// entry queue count 데이터가 없다면 생성
		if (redisTemplate.opsForValue().get(ENTRY_QUEUE_COUNT_KEY_NAME) == null) {
			redisTemplate.opsForValue()
				.set(ENTRY_QUEUE_COUNT_KEY_NAME, ENTRY_QUEUE_CAPACITY);
		}
		return redisTemplate;
	}

	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer(
		@Qualifier("pubSubConnectionFactory") LettuceConnectionFactory cf,
		MessageListenerAdapter listenerAdapter) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(cf);
		// 구독할 채널을 등록
		container.addMessageListener(listenerAdapter, new PatternTopic(RedisConfig.DISPATCH_QUEUE_CHANNEL_NAME));
		// container.start();
		return container;
	}

	@Bean
	public MessageListenerAdapter listenerAdapter(Subscriber subscriber) {
		// "onMessage" 메소드가 호출됩니다.
		return new MessageListenerAdapter(subscriber, "onMessage");
	}

}
