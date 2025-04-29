package org.codeNbug.mainserver.domain.seat.controller;

import org.codeNbug.mainserver.domain.seat.dto.SeatSelectRequest;
import org.codeNbug.mainserver.domain.seat.service.SeatService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/event")
public class SeatController {
	private SeatService seatService;

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
}
