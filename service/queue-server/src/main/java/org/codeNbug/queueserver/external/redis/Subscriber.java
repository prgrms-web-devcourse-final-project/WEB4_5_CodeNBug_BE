package org.codeNbug.queueserver.external.redis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

import org.codeNbug.queueserver.waitingqueue.entity.SseConnection;
import org.codeNbug.queueserver.waitingqueue.entity.Status;
import org.codeNbug.queueserver.waitingqueue.service.SseEmitterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class Subscriber implements MessageListener {

	private final ObjectMapper objectMapper;
	private final SseEmitterService sseEmitterService;
	private final RedisTemplate<String, Object> redisTemplate;

	@Value("${custom.instance-id}")
	private String instanceId;
	@Value("${jwt.secret}")
	private String secret;
	@Value("${jwt.expiration}")
	private long expiration;

	public Subscriber(ObjectMapper objectMapper, SseEmitterService sseEmitterService,
		RedisTemplate<String, Object> redisTemplate) {
		this.objectMapper = objectMapper;
		this.sseEmitterService = sseEmitterService;
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		Map<String, Object> body = null;
		try {
			body = objectMapper.readValue(message.getBody(),
				new TypeReference<>() {
				});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		String parsedInstanceId = body.get("instanceId")
			.toString()
			.substring(1, body.get("instanceId").toString().length() - 1);
		if (instanceId.equals(parsedInstanceId)) {
			Long userId = Long.parseLong(body.get("userId").toString());
			Long eventId = Long.parseLong(body.get("eventId").toString());
			SseConnection sseConnection = sseEmitterService.getEmitterMap().get(userId);
			sseConnection.setStatus(Status.IN_ENTRY);
			SseEmitter emitter = sseConnection.getEmitter();
			try {
				String token = createToken(Map.of("eventId", eventId, "userId", userId), userId.toString(), expiration);
				redisTemplate.opsForHash()
					.put(RedisConfig.ENTRY_TOKEN_STORAGE_KEY_NAME, userId.toString(), token);

				// eventId, userId로 jwt 생성
				// jwt를 같이 보내줘야 됨.
				// 동시에 redis에 저장

				emitter.send(
					SseEmitter.event()
						.data(Map.of(
							"eventId", eventId,
							"userId", userId,
							"status", sseConnection.getStatus(),
							"token", token
						))
				);
			} catch (Exception e) {
				emitter.complete();
			}

		}
	}

	private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
		return Jwts.builder()
			.setClaims(claims)
			.setSubject(subject)
			.setIssuedAt(new Date(System.currentTimeMillis()))
			.setExpiration(new Date(System.currentTimeMillis() + expirationTime))
			.signWith(getSigningKey(), SignatureAlgorithm.HS256)
			.compact();
	}

	private Key getSigningKey() {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

}
