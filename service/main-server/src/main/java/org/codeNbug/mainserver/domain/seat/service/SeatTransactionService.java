package org.codeNbug.mainserver.domain.seat.service;

import java.time.Duration;
import java.util.UUID;

import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codeNbug.mainserver.global.exception.globalException.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatTransactionService {

	private final SeatRepository seatRepository;
	private final RedisLockService redisLockService;
	private static final String SEAT_LOCK_KEY_PREFIX = "seat-lock:";

	@Transactional
	public void reserveSeat(Seat seat, Long userId, Long eventId, Long seatId) {
		String lockKey = SEAT_LOCK_KEY_PREFIX + userId + ":" + eventId + ":" + seatId;
		String lockValue = UUID.randomUUID().toString();

		boolean lockSuccess = redisLockService.tryLock(lockKey, lockValue, Duration.ofMinutes(5));
		if (!lockSuccess) {
			throw new BadRequestException("[reserveSeat] 이미 선택된 좌석이 있습니다.");
		}

		try {
			seat.reserve();
			seatRepository.save(seat);

			// 트랜잭션 성공 시에만 락 해제
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					if (status == STATUS_COMMITTED) {
						redisLockService.unlock(lockKey, lockValue);
					}
				}
			});

		} catch (Exception e) {
			// 예외 발생 시 즉시 락 해제
			redisLockService.unlock(lockKey, lockValue);
			throw e;
		}
	}
}