package org.codeNbug.queueserver.external.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

	private static final String WAITING_QUEUE_GROUP_NAME = "WAITING_QUEUE";
	private static final String WAITING_QUEUE_KEY_NAME = "WAITING";

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.opsForStream()
			.createGroup(WAITING_QUEUE_KEY_NAME, WAITING_QUEUE_GROUP_NAME);
		return redisTemplate;
	}
}
