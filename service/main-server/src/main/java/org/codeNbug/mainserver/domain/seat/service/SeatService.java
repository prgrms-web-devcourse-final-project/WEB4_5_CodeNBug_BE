package org.codeNbug.mainserver.domain.seat.service;

import java.time.Duration;
import java.util.UUID;

import org.codeNbug.mainserver.domain.seat.dto.SeatCancelRequest;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectRequest;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 좌석 도메인 관련 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class SeatService {

	private final RedisLockService redisLockService;
	private final SeatRepository seatRepository;

	private static final String SEAT_LOCK_KEY_PREFIX = "seat:lock:";

	/**
	 * 좌석 선택 요청에 따라 Redis 락을 걸고, DB에 좌석 예약을 반영
	 *
	 * @param eventId           좌석이 포함된 이벤트 ID
	 * @param seatSelectRequest 선택한 좌석 ID 목록을 포함한 요청 객체
	 * @throws IllegalStateException 이미 선택된 좌석이 있는 경우
	 * @throws IllegalArgumentException 존재하지 않는 좌석이 포함된 경우
	 */
	@Transactional
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

	@Transactional
	public void cancelSeat(Long eventId, SeatCancelRequest seatCancelRequest) {
		for (Long seatId : seatCancelRequest.getSeatList()) {
			String lockKey = SEAT_LOCK_KEY_PREFIX + eventId + ":" + seatId;

			Seat seat = seatRepository.findById(seatId)
				.orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다. seatId: " + seatId));
			seat.cancelReserve();
			redisLockService.unlock(lockKey, null);
		}
	}
}