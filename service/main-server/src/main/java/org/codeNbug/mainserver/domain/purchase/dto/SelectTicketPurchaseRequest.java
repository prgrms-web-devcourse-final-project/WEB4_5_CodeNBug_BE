package org.codeNbug.mainserver.domain.purchase.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SelectTicketPurchaseRequest {
	private Long purchaseId;
	private String paymentUuid;
	private String orderId;
	private String orderName;
	private Integer amount;
	private Long eventId;
	private List<Long> seatIds;
	private String paymentMethod;
}
