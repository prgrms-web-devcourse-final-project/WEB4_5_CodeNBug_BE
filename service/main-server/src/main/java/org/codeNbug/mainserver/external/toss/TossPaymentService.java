package org.codeNbug.mainserver.external.toss;

import java.io.IOException;

public interface TossPaymentService {
	ConfirmedPaymentInfo confirmPayment(String uuid, String orderId, String orderName, Integer amount)
		throws InterruptedException, IOException;
}