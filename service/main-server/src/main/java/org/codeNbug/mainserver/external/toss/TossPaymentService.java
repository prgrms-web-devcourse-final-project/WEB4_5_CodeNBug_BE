package org.codeNbug.mainserver.external.toss;

public interface TossPaymentService {
	ConfirmedPaymentInfo confirmPayment(String paymentUuid, String orderId, String orderName, String amount);
}