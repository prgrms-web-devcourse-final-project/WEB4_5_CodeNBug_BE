package org.codeNbug.mainserver.global.config;

import org.codeNbug.mainserver.domain.seat.service.RedisListenerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

	private final RedisListenerService redisListenerService;

	public RedisConfig(RedisListenerService redisListenerService) {
		this.redisListenerService = redisListenerService;
	}

	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);

		container.addMessageListener(redisListenerService, new ChannelTopic("__keyevent@0__:expired"));
		return container;
	}
}