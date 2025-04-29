package org.codeNbug.mainserver.domain.seat.dto;

import java.util.List;

import lombok.Getter;

@Getter
public class SeatCancelRequest {
	private List<Long> seatList;
}
