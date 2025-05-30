package org.codeNbug.queueserver.waitingqueue.service;

import static org.codeNbug.queueserver.external.redis.RedisConfig.*;

import java.util.Map;

import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.security.exception.AuthenticationFailedException;
import org.codenbug.user.security.service.CustomUserDetails;
import org.codenbug.user.security.service.SnsUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WaitingQueueEntryService {

	private final SseEmitterService sseEmitterService;
	private final RedisTemplate<String, Object> simpleRedisTemplate;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;
	private final RedisTemplate<Object, Object> redisTemplate;
	@Value("${custom.backend.url}")
	private String url;

	@Value("${custom.instance-id}")
	private String instanceId;

	public WaitingQueueEntryService(SseEmitterService sseEmitterService,
		RedisTemplate<String, Object> simpleRedisTemplate, UserRepository userRepository, ObjectMapper objectMapper,
		RedisTemplate<Object, Object> redisTemplate) {
		this.sseEmitterService = sseEmitterService;
		this.simpleRedisTemplate = simpleRedisTemplate;
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
		this.redisTemplate = redisTemplate;
	}

	public SseEmitter entry(Long eventId) throws JsonProcessingException {
		// 로그인한 유저 id 조회
		Long id = getLoggedInUserId();

		// emitter 생성 및 저장
		SseEmitter emitter = sseEmitterService.add(id, eventId);

		// TODO: waiting thread에 유저를 추가하도록 전달
		enter(id, eventId);
		return emitter;
	}

	/**
	 * 사용자를 대기열에 추가한다. redis의 메시지 idx를 가져와 메시지의 idx로 추가하고 1을 증가시킨다.
	 * 이후 WAITING stream에 메시지를 추가해 사용자를 대기열에 추가한다.
	 *
	 * @param userId 대기열에 추가할 유저 id
	 * @param eventId 행사의 id
	 */
	private void enter(Long userId, Long eventId) throws JsonProcessingException {
		// 총 좌석수 얻기
		RestTemplate restTemplate = new RestTemplate();

		ResponseEntity<String> forEntity = restTemplate.getForEntity(
			url + "/api/v1/events/" + eventId, String.class);

		System.out.println("forEntity.getBody().toString() = " + forEntity.getBody().toString());

		int seatCount = objectMapper.readTree(forEntity.getBody())
			.get("data")
			.get("information")
			.get("seatCount")
			.asInt();

		if (!simpleRedisTemplate.opsForHash().hasKey(ENTRY_QUEUE_COUNT_KEY_NAME, eventId.toString())) {
			simpleRedisTemplate.opsForHash()
				.put(ENTRY_QUEUE_COUNT_KEY_NAME, eventId.toString(), seatCount);
		}


		// 대기열 큐 idx 추가
		Long idx = simpleRedisTemplate.opsForHash()
			.increment(WAITING_QUEUE_IDX_KEY_NAME, eventId.toString(), 1);

		// 유저가 대기열에 있었는지 확인하기 위한 hash 값 조회
		Boolean isEntered = simpleRedisTemplate.opsForHash()
			.hasKey(WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + eventId, userId.toString());

		if (isEntered) {
			return;
		}

		assert idx != null;

		// { idx: "idx", userId: "userId", eventId: "eventId", "instanceId": instanceId}
		// 로 key가 WAITING인 stream에 저장하고 recordId를 반환
		RecordId recordId = simpleRedisTemplate.opsForStream()
			.add(StreamRecords.mapBacked(
				Map.of(QUEUE_MESSAGE_IDX_KEY_NAME, idx, QUEUE_MESSAGE_USER_ID_KEY_NAME, userId,
					QUEUE_MESSAGE_EVENT_ID_KEY_NAME, eventId, QUEUE_MESSAGE_INSTANCE_ID_KEY_NAME, instanceId)
			).withStreamKey(WAITING_QUEUE_KEY_NAME + ":" + eventId.toString()));

		assert recordId != null;
		// 유저가 대기열에 있는지 확인하기 위한 hash 값 업데이트
		simpleRedisTemplate.opsForHash()
			.put(WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + eventId, userId.toString(), recordId.getValue());

	}

	private Long getLoggedInUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AuthenticationFailedException("인증된 사용자를 찾을 수 없습니다.");
		}

		Object principal = authentication.getPrincipal();

		if (principal instanceof CustomUserDetails) {
			return ((CustomUserDetails)principal).getUserId();
		} else if (principal instanceof SnsUserDetails) {
			return ((SnsUserDetails)principal).getUserId();
		} else {
			throw new AuthenticationFailedException("지원되지 않는 사용자 유형입니다.");
		}
	}
}
