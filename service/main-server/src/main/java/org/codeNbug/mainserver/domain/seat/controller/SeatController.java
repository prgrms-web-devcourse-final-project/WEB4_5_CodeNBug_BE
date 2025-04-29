package org.codeNbug.mainserver.domain.seat.controller;

import org.codeNbug.mainserver.domain.seat.dto.SeatCancelRequest;
import org.codeNbug.mainserver.domain.seat.dto.SeatSelectRequest;
import org.codeNbug.mainserver.domain.seat.service.SeatService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class SeatController {
	private SeatService seatService;

	/**
	 * 좌석 선택 API
	 *
	 * @param eventId           좌석이 포함된 이벤트 ID
	 * @param seatSelectRequest 사용자가 선택한 좌석 ID 목록
	 * @return 좌석 선택 결과 응답
	 */
	@PostMapping("/{event-id}/seats")
	public ResponseEntity<RsData> selectSeat(@PathVariable("event-id") Long eventId,
		@RequestBody SeatSelectRequest seatSelectRequest) {
		try {
			seatService.selectSeat(eventId, seatSelectRequest);
			RsData response = new RsData(
				"200",
				"좌석 선택 성공",
				seatSelectRequest
			);
			return ResponseEntity.ok(response);
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(new RsData("409", "이미 선택된 좌석입니다."));
		}
	}

	/**
	 * 좌석 취소 API
	 *
	 * @param eventId           좌석이 포함된 이벤트 ID
	 * @param seatCancelRequest 사용자가 취소한 좌석 ID 목록
	 * @return 좌석 취소 결과 응답
	 */
	@DeleteMapping("/{event-id}/seats")
	public ResponseEntity<RsData> CancelSeat(@PathVariable("event-id") Long eventId,
		@RequestBody SeatCancelRequest seatCancelRequest) {
		try {
			seatService.cancelSeat(eventId, seatCancelRequest);
			RsData response = new RsData(
				"200",
				"좌석 취소 성공"
			);
			return ResponseEntity.ok(response);
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new RsData("404", "해당 좌석을 찾을 수 없습니다."));
		}
	}
}
