package org.codeNbug.mainserver.domain.purchase.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SelectTicketPurchaseRequest implements TicketPurchaseRequest {
	private Long purchaseId;
	private Long eventId;
	private List<Long> seatIds;

	@Override
	public int getTicketCount() {
		return seatIds != null ? seatIds.size() : 0;
	}
}