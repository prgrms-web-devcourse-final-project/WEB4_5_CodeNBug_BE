package org.codeNbug.mainserver.domain.purchase.dto;

import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.purchase.entity.PaymentMethodEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentResponse {
	private String paymentKey;
	private String orderId;
	private String orderName;
	private int totalAmount;
	private String status;
	private PaymentMethodEnum method;
	private LocalDateTime approvedAt;
	private Receipt receipt;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Receipt {
		private String url;
	}
}
