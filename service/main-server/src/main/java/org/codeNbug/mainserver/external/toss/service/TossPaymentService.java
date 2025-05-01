package org.codeNbug.mainserver.external.toss.service;

import java.io.IOException;

import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;

public interface TossPaymentService {
	ConfirmedPaymentInfo confirmPayment(String uuid, String orderId, String orderName, Integer amount)
		throws InterruptedException, IOException;
}