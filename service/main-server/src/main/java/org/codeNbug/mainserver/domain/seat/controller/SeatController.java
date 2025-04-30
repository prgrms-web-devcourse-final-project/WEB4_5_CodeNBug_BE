package org.codeNbug.mainserver.domain.seat.controller;

import lombok.RequiredArgsConstructor;
import org.codeNbug.mainserver.domain.seat.dto.SeatCancelRequest;
import org.codeNbug.mainserver.domain.seat.dto.SeatLayoutResponse;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectRequest;
import org.codeNbug.mainserver.domain.seat.service.SeatService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codeNbug.mainserver.global.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 좌석 도메인 관련 요청을 처리하는 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/event")
@RequiredArgsConstructor
public class SeatController {
	private final SeatService seatService;

	/**
	 * 좌석 조회 API
	 *
	 * @param eventId 조회할 이벤트 ID
	 * @return 좌석 선택 결과 응답
	 */
	@GetMapping("/{event-id}/seats")
	public ResponseEntity<RsData<SeatLayoutResponse>> getSeatLayout(@PathVariable("event-id") Long eventId) {
		Long userId = SecurityUtil.getCurrentUserId();
		SeatLayoutResponse seatLayoutResponse = seatService.getSeatLayout(eventId, userId);
		return ResponseEntity.ok(new RsData<>(
				"200",
				"좌석 조회 성공",
				seatLayoutResponse
		));
	}

	/**
	 * 좌석 선택 API
	 *
	 * @param eventId           조회할 이벤트 ID
	 * @param seatSelectRequest 사용자가 선택한 좌석 ID 목록
	 * @return 좌석 선택 결과 응답
	 */
	@PostMapping("/{event-id}/seats")
	public ResponseEntity<RsData> selectSeat(@PathVariable("event-id") Long eventId,
		@RequestBody SeatSelectRequest seatSelectRequest) {
		Long userId = SecurityUtil.getCurrentUserId();
		seatService.selectSeat(eventId, seatSelectRequest, userId);
		RsData response = new RsData(
			"200",
			"좌석 선택 성공",
			seatSelectRequest
		);
		return ResponseEntity.ok(response);
	}

	/**
	 * 좌석 취소 API
	 *
	 * @param eventId           조회할 이벤트 ID
	 * @param seatCancelRequest 사용자가 취소한 좌석 ID 목록
	 * @return 좌석 취소 결과 응답
	 */
	@DeleteMapping("/{event-id}/seats")
	public ResponseEntity<RsData> CancelSeat(@PathVariable("event-id") Long eventId,
		@RequestBody SeatCancelRequest seatCancelRequest) {
		Long userId = SecurityUtil.getCurrentUserId();
		seatService.cancelSeat(eventId, seatCancelRequest, userId);
		RsData response = new RsData(
			"200",
			"좌석 취소 성공"
		);
		return ResponseEntity.ok(response);
	}
}
