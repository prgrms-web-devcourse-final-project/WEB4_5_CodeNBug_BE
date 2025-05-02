package org.codeNbug.mainserver.domain.seat.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SeatSelectRequest {
	private List<Long> seatList;
	private Integer ticketCount;
}
