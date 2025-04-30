package org.codeNbug.mainserver.domain.purchase.dto;

import java.util.List;

import lombok.Getter;

@Getter
public class SelectTicketPurchaseRequest {
	private String paymentUuid;
	private String orderId;
	private String orderName;
	private Integer amount;
	private Long eventId;
	private List<Long> seatIds;
	private String paymentMethod;
}
