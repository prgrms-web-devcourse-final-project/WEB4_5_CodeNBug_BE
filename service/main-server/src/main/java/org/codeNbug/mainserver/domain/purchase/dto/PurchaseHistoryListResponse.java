package org.codeNbug.mainserver.domain.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PurchaseHistoryListResponse {
    private List<PurchaseSummaryDto> purchases;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

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

    public static PurchaseHistoryListResponse of(Page<PurchaseSummaryDto> page) {
        return PurchaseHistoryListResponse.builder()
                .purchases(page.getContent())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
} 