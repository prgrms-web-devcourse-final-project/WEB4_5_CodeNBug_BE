package org.codenbug.messagedispatcher.thread;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class ThreadConfig {
	@Bean(name = "singleThreadExecutor")
	public Executor singleThreadExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1); // 스레드 1개만 사용
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(1000); // 요청 버퍼 크기
		executor.setThreadNamePrefix("single-thread-");
		executor.initialize();
		return executor;
	}
}
