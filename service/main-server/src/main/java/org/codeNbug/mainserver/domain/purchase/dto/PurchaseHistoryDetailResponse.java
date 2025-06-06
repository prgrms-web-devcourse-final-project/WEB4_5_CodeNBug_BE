package org.codeNbug.mainserver.domain.purchase.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PurchaseHistoryDetailResponse {
	private List<PurchaseDto> purchases; // 구매 이력 목록 -> PurchaseDto를 통해 구매이력 객체를 리스트로 반환 의도

	@Getter
	@Builder
	@AllArgsConstructor
	public static class PurchaseDto {
		private Long purchaseId;
		private String paymentKey;
		private Long eventId;
		private String itemName;
		private Integer amount;
		private LocalDateTime purchaseDate;
		private String paymentMethod;
		private String paymentStatus;
		private List<TicketInfo> tickets;
	}

	@Getter
	@Builder
	@AllArgsConstructor
	public static class TicketInfo {
		private Long ticketId;
		private String seatLocation;
	}
} 