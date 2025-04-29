package org.codeNbug.mainserver.domain.purchase.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NonSelectTicketPurchaseResponse {
	private Long purchaseId;
	private Long eventId;
	private Long userId;
	private List<TicketDto> tickets;
	private Integer ticketCount;
	private Integer totalAmount;
	private String paymentStatus;
	private LocalDateTime purchaseDate;

	@Getter
	@AllArgsConstructor
	public static class TicketDto {
		private Long ticketId;
	}

	public static NonSelectTicketPurchaseResponse from(
		Purchase purchase,
		List<Ticket> ticketList
	) {
		return NonSelectTicketPurchaseResponse.builder()
			.purchaseId(purchase.getId())
			.eventId(ticketList.getFirst().getEvent().getEventId())
			.userId(purchase.getUser().getUserId())
			.tickets(ticketList.stream()
				.map(ticket -> new TicketDto(ticket.getId()))
				.collect(Collectors.toList()))
			.ticketCount(ticketList.size())
			.totalAmount(purchase.getTotalAmount())
			.paymentStatus(purchase.getPaymentStatus().name())
			.purchaseDate(purchase.getPurchaseDate())
			.build();
	}
}
