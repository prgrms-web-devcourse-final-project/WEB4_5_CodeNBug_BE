package org.codeNbug.mainserver.domain.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PurchaseHistoryListResponse {
    private List<PurchaseSummaryDto> purchases;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PurchaseSummaryDto {
        private Long purchaseId;
        private String itemName;
        private Integer amount;
        private LocalDateTime purchaseDate;
        private String paymentMethod;
        private String paymentStatus;
    }
} 