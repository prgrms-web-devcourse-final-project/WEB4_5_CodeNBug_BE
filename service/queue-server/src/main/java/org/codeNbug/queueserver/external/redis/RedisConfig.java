package org.codeNbug.queueserver.external.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	// 컨슈머 생성 시 그룹 이름
	public static final String WAITING_QUEUE_GROUP_NAME = "WAITING_QUEUE";
	// redis stream 키 이름
	public static final String WAITING_QUEUE_KEY_NAME = "WAITING";
	// 메시지의 idx 값을 저장하기 위한 space의 key값
	public static final String WAITING_QUEUE_IDX_KEY_NAME = "WAITING_QUEUE_IDX";
	// 메시지 내부의 userId 속성의 키 값
	public static final String WAITING_QUEUE_MESSAGE_USER_ID_KEY_NAME = "usrId";
	// 메시지 내부의 eventId 속성의 키 값
	public static final String WAITING_QUEUE_MESSAGE_EVENT_ID_KEY_NAME = "eventId";
	// 메시지 내부의 idx 속성의 키 값
	public static final String WAITING_QUEUE_MESSAGE_IDX_KEY_NAME = "idx";

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
			.groups(WAITING_QUEUE_KEY_NAME).isEmpty()) {
			redisTemplate.opsForStream()
				.createGroup(WAITING_QUEUE_KEY_NAME, WAITING_QUEUE_GROUP_NAME);
		}
		// 메시지 idx 데이터가 없다면 생성.
		if (redisTemplate.opsForValue().get(WAITING_QUEUE_IDX_KEY_NAME) == null) {
			redisTemplate.opsForValue()
				.set(WAITING_QUEUE_IDX_KEY_NAME, 0L);
		}
		return redisTemplate;
	}

}
