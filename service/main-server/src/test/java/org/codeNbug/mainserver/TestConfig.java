package org.codeNbug.mainserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class TestConfig {
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	static {
		GenericContainer redisContainer = new GenericContainer(DockerImageName.parse("redis:alpine"))
			.withExposedPorts(6379)
			.withReuse(true);

		redisContainer.start();
		System.setProperty("spring.data.redis.host", redisContainer.getHost());
		System.setProperty("spring.data.redis.port", redisContainer.getMappedPort(6379).toString());

		GenericContainer mysqlContainer = new GenericContainer(DockerImageName.parse("mysql/mysql-server:8.0"))
			.withExposedPorts(3306)
			.withReuse(true);
		mysqlContainer.start();
		System.setProperty("spring.datasource.url",
			"jdbc:mysql://" + mysqlContainer.getHost() + ":" + mysqlContainer.getMappedPort(3306) + "/ticketonTest");
	}
}
