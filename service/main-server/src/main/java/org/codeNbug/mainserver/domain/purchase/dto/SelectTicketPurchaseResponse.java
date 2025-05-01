package org.codeNbug.mainserver.domain.purchase.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SelectTicketPurchaseResponse {
	private Long purchaseId;
	private Long eventId;
	private Long userId;
	private List<TicketInfo> tickets;
	private Integer ticketCount;
	private Integer amount;
	private String paymentStatus;
	private LocalDateTime purchaseDate;

	@Getter
	@AllArgsConstructor
	public static class TicketInfo {
		private Long ticketId;
		private Long seatId;
	}
}
