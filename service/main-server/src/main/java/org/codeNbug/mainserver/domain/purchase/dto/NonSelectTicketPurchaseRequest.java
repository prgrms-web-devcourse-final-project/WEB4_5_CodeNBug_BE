package org.codeNbug.mainserver.domain.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NonSelectTicketPurchaseRequest {
	private String paymentUuid;
	private String orderId;
	private String orderName;
	private String amount;
	private Long eventId;
	private int ticketCount;
	private String paymentMethod;
}