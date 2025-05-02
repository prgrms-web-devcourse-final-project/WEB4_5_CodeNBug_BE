package org.codeNbug.mainserver.domain.seat.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.seat.dto.SeatCancelRequest;
import org.codeNbug.mainserver.domain.seat.dto.SeatLayoutResponse;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectRequest;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectResponse;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
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
	private final EventRepository eventRepository;
	private final SeatLayoutRepository seatLayoutRepository;

	private static final String SEAT_LOCK_KEY_PREFIX = "seat:lock:";

	/**
	 * 주어진 이벤트 ID에 해당하는 좌석 목록 조회
	 *
	 * @param eventId 조회할 이벤트 ID
	 * @return SeatLayoutResponse 좌석 레이아웃
	 * @throws IllegalArgumentException 존재하지 않는 이벤트 ID일 경우
	 */
	public SeatLayoutResponse getSeatLayout(Long eventId, Long userId) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("로그인된 사용자가 없습니다.");
		}
		SeatLayout seatLayout = seatLayoutRepository.findByEvent_EventId(eventId)
			.orElseThrow(() -> new IllegalArgumentException("행사가 존재하지 않습니다."));

		List<Seat> seatList = seatRepository.findAllByLayoutIdWithGrade((seatLayout.getId()));
		return new SeatLayoutResponse(seatList);
	}

	/**
	 * 좌석 선택 요청에 따라 Redis 락을 걸고, DB에 좌석 상태 반영
	 *
	 * @param eventId           이벤트 ID
	 * @param seatSelectRequest 선택한 좌석 ID 목록을 포함한 요청 객체
	 *      @param userId           유저 ID
	 * @throws IllegalStateException 이미 선택된 좌석이 있는 경우
	 * @throws IllegalArgumentException 존재하지 않는 좌석이 포함된 경우
	 */
	@Transactional
	public SeatSelectResponse selectSeat(Long eventId, SeatSelectRequest seatSelectRequest, Long userId) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("로그인된 사용자가 없습니다.");
		}

		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new IllegalArgumentException("행사가 존재하지 않습니다."));
		if (!event.getSeatSelectable()) {
			throw new IllegalStateException("이 이벤트는 좌석 선택이 불가능합니다.");
		}

		List<Long> selectedSeats = seatSelectRequest.getSeatList();
		if (selectedSeats.size() > 4) {
			throw new IllegalArgumentException("최대 4개의 좌석만 선택할 수 있습니다.");
		}

		for (Long seatId : seatSelectRequest.getSeatList()) {
			Seat seat = seatRepository.findById(seatId)
				.orElseThrow(() -> new IllegalArgumentException("좌석이 존재하지 않습니다."));

			if (!seat.isAvailable()) {
				throw new IllegalStateException("이미 예매된 좌석입니다. seatId = " + seatId);
			}

			String lockKey = SEAT_LOCK_KEY_PREFIX + userId + ":" + eventId + ":" + seatId;
			String lockValue = UUID.randomUUID().toString();

			boolean lockSuccess = redisLockService.tryLock(lockKey, lockValue, Duration.ofMinutes(5));
			if (!lockSuccess) {
				throw new IllegalStateException("이미 선택된 좌석이 있습니다.");
			}

			seat.reserve();
			seatRepository.save(seat);
		}
		SeatSelectResponse seatSelectResponse = new SeatSelectResponse();
		seatSelectResponse.setSeatList(seatSelectRequest.getSeatList());
		return seatSelectResponse;
	}

	/**
	 * 좌석 취소 요청에 따라 Redis 락을 해제하고, DB에 좌석 상태 반영
	 *
	 * @param eventId           이벤트 ID
	 * @param seatCancelRequest 선택한 좌석 ID 목록을 포함한 요청 객체
	 * @param userId            유저 ID
	 * @throws IllegalArgumentException 존재하지 않는 좌석이 포함된 경우
	 */
	@Transactional
	public void cancelSeat(Long eventId, SeatCancelRequest seatCancelRequest, Long userId) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("로그인된 사용자가 없습니다.");
		}

		for (Long seatId : seatCancelRequest.getSeatList()) {
			String lockKey = SEAT_LOCK_KEY_PREFIX + userId + ":" + eventId + ":" + seatId;

			String lockValue = redisLockService.getLockValue(lockKey);
			if (lockValue == null || !redisLockService.unlock(lockKey, lockValue)) {
				throw new IllegalStateException("좌석 락을 해제할 수 없습니다.");
			}

			Seat seat = seatRepository.findById(seatId)
				.orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다. seatId: " + seatId));
			seat.cancelReserve();
			redisLockService.unlock(lockKey, null);
		}
	}
}