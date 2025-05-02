package org.codenbug.messagedispatcher.redis;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

@Configuration
public class RedisConfig {

	// waiting queue 컨슈머 생성 시 그룹 이름
	public static final String WAITING_QUEUE_GROUP_NAME = "WAITING_QUEUE";
	public static final String WAITING_QUEUE_CONSUMER_NAME = "WAITING_QUEUE_CONSUMER";
	// waiting queue redis stream 키 이름
	public static final String WAITING_QUEUE_KEY_NAME = "WAITING";
	// entry queue 컨슈머 생성 시 그룹 이름
	public static final String ENTRY_QUEUE_GROUP_NAME = "ENTRY_QUEUE";
	// entry queue redis stream 키 이름
	public static final String ENTRY_QUEUE_KEY_NAME = "ENTRY";
	// entry queue의 현재 인원을  저장하기 위한 space의 key값
	public static final String ENTRY_QUEUE_COUNT_KEY_NAME = "ENTRY_QUEUE_COUNT";
	// dispatch queue의 컨슈머 그룹명
	public static final String ENTRY_QUEUE_CONSUMER_NAME = "ENTRY_QUEUE_CONSUMER";
	public static final String DISPATCH_QUEUE_CHANNEL_NAME = "DISPATCH";
	public static final Long ENTRY_QUEUE_CAPACITY = 1000L;
	public static final String WAITING_QUEUE_IN_USER_RECORD_KEY_NAME = "WAITING_USER_ID";

	@Bean
	public RedisTemplate<String, Object> basicRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> redisTemplate = getRedisTemplate(redisConnectionFactory);
		redisTemplate.afterPropertiesSet();

		// 컨슈머 그룹이 없으면 새로운 컨슈머 그룹 생성
		try {
			if (redisTemplate.opsForStream()
				.groups(WAITING_QUEUE_KEY_NAME)
				.stream()
				.noneMatch(xInfoGroup -> xInfoGroup.groupName().equals(WAITING_QUEUE_GROUP_NAME))) {
				redisTemplate.opsForStream().createGroup(WAITING_QUEUE_KEY_NAME, WAITING_QUEUE_GROUP_NAME);
			}

		} catch (Exception e) {
			redisTemplate.opsForStream().createGroup(WAITING_QUEUE_KEY_NAME, WAITING_QUEUE_GROUP_NAME);
		}
		try {
			if (redisTemplate.opsForStream()
				.groups(ENTRY_QUEUE_KEY_NAME)
				.stream()
				.noneMatch(xInfoGroup -> xInfoGroup.groupName().equals(ENTRY_QUEUE_GROUP_NAME))) {
				redisTemplate.opsForStream().createGroup(ENTRY_QUEUE_KEY_NAME, ENTRY_QUEUE_GROUP_NAME);
			}
		} catch (Exception e) {
			redisTemplate.opsForStream().createGroup(ENTRY_QUEUE_KEY_NAME, ENTRY_QUEUE_GROUP_NAME);
		}

		return redisTemplate;
	}

	private static RedisTemplate<String, Object> getRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		StringRedisSerializer keySerializer = new StringRedisSerializer();
		// 메시지 필드와 값은 JSON
		GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setKeySerializer(keySerializer);
		redisTemplate.setHashKeySerializer(keySerializer);
		redisTemplate.setHashValueSerializer(jsonSerializer);
		redisTemplate.setValueSerializer(jsonSerializer);
		return redisTemplate;
	}

	@Bean
	public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamContainer(
		RedisConnectionFactory cf) {

		StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options
			= StreamMessageListenerContainerOptions.builder()
			.batchSize(10)
			.pollTimeout(Duration.ofSeconds(2))
			.build();

		StreamMessageListenerContainer<String, MapRecord<String, String, String>> container
			= StreamMessageListenerContainer.create(cf, options);
		container.start();
		return container;
	}
}
