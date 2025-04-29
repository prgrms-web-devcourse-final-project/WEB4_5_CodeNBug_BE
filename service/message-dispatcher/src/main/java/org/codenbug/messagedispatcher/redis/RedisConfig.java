package org.codenbug.messagedispatcher.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	// waiting queue 컨슈머 생성 시 그룹 이름
	public static final String WAITING_QUEUE_GROUP_NAME = "WAITING_QUEUE";
	// waiting queue redis stream 키 이름
	public static final String WAITING_QUEUE_KEY_NAME = "WAITING";
	// entry queue 컨슈머 생성 시 그룹 이름
	public static final String ENTRY_QUEUE_GROUP_NAME = "ENTRY_QUEUE";
	// entry queue redis stream 키 이름
	public static final String ENTRY_QUEUE_KEY_NAME = "ENTRY";
	// 메시지의 idx 값을 저장하기 위한 space의 key값
	public static final String WAITING_QUEUE_IDX_KEY_NAME = "WAITING_QUEUE_IDX";
	// entry queue의 현재 인원을  저장하기 위한 space의 key값
	public static final String ENTRY_QUEUE_COUNT_KEY_NAME = "ENTRY_QUEUE_COUNT";
	// dispatch queue의 컨슈머 그룹명
	public static final String DISPATCH_QUEUE_GROUP_NAME = "DISPATCH_QUEUE";
	public static final String DISPATCH_QUEUE_KEY_NAME = "DISPATCH";
	// 메시지 내부의 userId 속성의 키 값
	public static final String QUEUE_MESSAGE_USER_ID_KEY_NAME = "userId";
	// 메시지 내부의 eventId 속성의 키 값
	public static final String QUEUE_MESSAGE_EVENT_ID_KEY_NAME = "eventId";
	// 메시지 내부의 idx 속성의 키 값
	public static final String QUEUE_MESSAGE_IDX_KEY_NAME = "idx";

	@Value("${custom.instance-id}")
	private String instanceId;

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
		if (redisTemplate.opsForStream()
			.groups(ENTRY_QUEUE_GROUP_NAME).stream().anyMatch(
				xInfoGroup -> xInfoGroup.groupName().equals(ENTRY_QUEUE_GROUP_NAME)
			)) {
			redisTemplate.opsForStream()
				.createGroup(ENTRY_QUEUE_GROUP_NAME, ENTRY_QUEUE_GROUP_NAME);
		}

		return redisTemplate;
	}

}
