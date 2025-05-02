package org.codeNbug.mainserver.domain.purchase.dto;

public interface TicketPurchaseRequest {
	Long getPurchaseId();

	Long getEventId();

	int getTicketCount();
}
