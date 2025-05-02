package org.codeNbug.mainserver.domain.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentRequest {
	private Long purchaseId;
	private String paymentKey;
	private String orderId;
	private Integer amount;
}