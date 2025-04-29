package org.codeNbug.mainserver.domain.seat.service;

import java.time.Duration;
import java.util.UUID;

import org.codeNbug.mainserver.domain.seat.dto.SeatSelectRequest;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatService {

	private final RedisLockService redisLockService;
	private final SeatRepository seatRepository;

	private static final String SEAT_LOCK_KEY_PREFIX = "seat:lock:";

	public void selectSeat(Long eventId, SeatSelectRequest seatSelectRequest) {
		for (Long seatId : seatSelectRequest.getSeatList()) {
			String lockKey = SEAT_LOCK_KEY_PREFIX + eventId + ":" + seatId;
			String lockValue = UUID.randomUUID().toString();

			boolean lockSuccess = redisLockService.tryLock(lockKey, lockValue, Duration.ofMinutes(5));
			if (!lockSuccess) {
				throw new IllegalStateException("이미 선택된 좌석이 있습니다.");
			}

			Seat seat = seatRepository.findById(seatId)
				.orElseThrow(() -> new IllegalArgumentException("좌석이 존재하지 않습니다."));

			seat.reserve();
			seatRepository.save(seat);

			redisLockService.unlock(lockKey, lockValue);
		}
	}
}