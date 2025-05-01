package org.codeNbug.mainserver.external.toss.service;

import java.io.IOException;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.codeNbug.mainserver.external.toss.dto.ConfirmedPaymentInfo;

public interface TossPaymentService {
	ConfirmedPaymentInfo confirmPayment(String uuid, String orderId, String orderName, Integer amount, String status)
		throws InterruptedException, IOException;

	Event getEvent(Long eventId);

	User getUser(Long userId);
}