package org.codeNbug.mainserver.domain.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NonSelectTicketPurchaseRequest {
	private Long purchaseId;
	private String paymentUuid;
	private String orderId;
	private String orderName;
	private Integer amount;
	private Long eventId;
	private int ticketCount;
	private String paymentMethod;
}