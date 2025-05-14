package org.codeNbug.mainserver;

import org.codenbug.user.security.config.CorsProperties;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(CorsProperties.class) // CORS 설정을 위한 프로퍼티 클래스 활성화 (임시)
@EnableScheduling      // 스케줄러 사용 가능
@EnableBatchProcessing // 스프링 배치 기능 활성화
public class MainServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MainServerApplication.class, args);
	}

}
