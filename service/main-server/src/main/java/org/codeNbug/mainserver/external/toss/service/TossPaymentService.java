package org.codeNbug.mainserver.external.toss.service;

import java.io.IOException;

import org.codeNbug.mainserver.external.toss.dto.CanceledPaymentInfo;
import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;

public interface TossPaymentService {
	ConfirmedPaymentInfo confirmPayment(String paymentKey, String orderId, Integer amount)
		throws InterruptedException, IOException;

	CanceledPaymentInfo cancelPayment(String paymentKey, String cancelReason); // 전액취소

	CanceledPaymentInfo cancelPartialPayment(String paymentKey, String cancelReason, Integer cancelAmount); // 부분취소
}