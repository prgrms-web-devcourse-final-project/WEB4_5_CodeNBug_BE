package org.codeNbug.mainserver.domain.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NonSelectTicketPurchaseRequest implements TicketPurchaseRequest {
	private Long purchaseId;
	private Long eventId;
	private int ticketCount;
}