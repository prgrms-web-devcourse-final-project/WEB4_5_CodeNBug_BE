package org.codeNbug.mainserver.domain.seat.dto;

import java.util.List;

import lombok.Setter;

@Setter
public class SeatSelectResponse {
	private List<Long> seatList;
}
