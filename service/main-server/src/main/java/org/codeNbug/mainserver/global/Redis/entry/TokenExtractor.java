package org.codeNbug.mainserver.global.Redis.entry;

import org.springframework.security.access.AccessDeniedException;

public class TokenExtractor {

	public static String extractBearer(String authorizationHeader) {
		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			return authorizationHeader.substring(7);
		}
		throw new AccessDeniedException("Authorization 헤더가 유효하지 않습니다.");
	}
}