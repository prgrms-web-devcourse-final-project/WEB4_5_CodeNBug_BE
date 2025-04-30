package org.codeNbug.mainserver.domain.seat.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SeatCancelRequest {
	private List<Long> seatList;
}
