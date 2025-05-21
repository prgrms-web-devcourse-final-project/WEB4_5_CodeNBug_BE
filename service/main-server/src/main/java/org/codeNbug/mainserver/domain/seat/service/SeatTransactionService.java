package org.codeNbug.mainserver.domain.seat.service;

import java.time.Duration;
import java.util.UUID;

import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatTransactionService {

	private final SeatRepository seatRepository;
	private final RedisLockService redisLockService;
	private static final String SEAT_LOCK_KEY_PREFIX = "seat:lock:";
	private final RedisTemplate<String, Object> redisTemplate;

	@Transactional
	public void reserveSeat(Seat seat, Long userId, Long eventId, Long seatId) {
		String lockKey = SEAT_LOCK_KEY_PREFIX + userId + ":" + eventId + ":" + seatId;
		String lockValue = UUID.randomUUID().toString();
		log.info("lock key: {}, lock value = {}", lockKey, lockValue);

		boolean lockSuccess = redisLockService.tryLock(lockKey, lockValue, Duration.ofMinutes(5));
		log.info("lock success: {}", lockSuccess);
		if (!lockSuccess) {
			throw new BadRequestException("[reserveSeat] 이미 선택된 좌석이 있습니다.");
		}

		try {
			seat.reserve();
			seatRepository.save(seat);
			log.info("[reserveSeat] seat id {}가 예매되었습니다. 예매 상태: {}", seat.getId(), seat.isAvailable());

			String cacheKey = "seatLayout:" + eventId;
			redisTemplate.delete(cacheKey);
			log.info("[reserveSeat] 행사 id {} 정보가 redis cache에서 삭제되었습니다.", eventId);
		} catch (Exception e) {
			// 예외 발생 시 즉시 락 해제
			redisLockService.unlock(lockKey, lockValue);
			throw e;
		}
	}
}