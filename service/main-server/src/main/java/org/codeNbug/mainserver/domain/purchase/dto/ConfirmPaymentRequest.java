package org.codeNbug.mainserver.domain.purchase.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentRequest {
	private String paymentKey;
	private String orderId;
	private Integer amount;
	private Long eventId;
	private Integer ticketCount;
	private List<Long> seatList;
}