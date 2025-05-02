package org.codeNbug.queueserver.entryauth.service;

import java.util.Map;

import org.codeNbug.queueserver.global.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EntryAuthService {
	@Value("${jwt.secret}")
	private String secret;
	@Value("${jwt.expiration}")
	private long expiration;

	public String generateEntryAuthToken(Map<String, Object> claims, String subject) {
		return JwtUtil.createToken(claims, secret, subject, expiration);
	}
}
