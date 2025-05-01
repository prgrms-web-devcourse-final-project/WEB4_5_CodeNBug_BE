package org.codeNbug.mainserver.external.toss;

import java.io.IOException;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.user.entity.User;

public interface TossPaymentService {
	ConfirmedPaymentInfo confirmPayment(String uuid, String orderId, String orderName, Integer amount)
		throws InterruptedException, IOException;

	Event getEvent(Long eventId);

	User getUser(Long userId);
}