package org.codeNbug.mainserver.domain.seat.dto;

import java.util.List;

import lombok.Getter;

/**
 * SecurityConfig 생성자
 */
@Getter
public class SeatSelectRequest {
	private List<Long> seatList;
}
