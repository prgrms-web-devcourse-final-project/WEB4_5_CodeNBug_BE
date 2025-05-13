package org.codeNbug.mainserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
	//
	// @Container
	// static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0.34")
	// 	.withDatabaseName("ticketoneTest")
	// 	.withUsername("test")
	// 	.withPassword("password");
	//
	// static {
	// 	GenericContainer redisContainer = new GenericContainer(DockerImageName.parse("redis:alpine"))
	// 		.withExposedPorts(6379)
	// 		.withReuse(true);
	//
	// 	redisContainer.start();
	// 	System.setProperty("spring.data.redis.host", redisContainer.getHost());
	// 	System.setProperty("spring.data.redis.port", redisContainer.getMappedPort(6379).toString());
	// }
	//
	// @DynamicPropertySource
	// static void mysqlProps(DynamicPropertyRegistry registry) {
	// 	registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
	// 	registry.add("spring.datasource.username", mySQLContainer::getUsername);
	// 	registry.add("spring.datasource.password", mySQLContainer::getPassword);
	// }
}
