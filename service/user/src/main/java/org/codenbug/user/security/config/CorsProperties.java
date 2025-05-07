package org.codenbug.user.security.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CORS 설정을 위한 프로퍼티 클래스
 */
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

	private List<String> allowedOrigins;

	public List<String> getAllowedOrigins() {
		return allowedOrigins;
	}

	public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}
}