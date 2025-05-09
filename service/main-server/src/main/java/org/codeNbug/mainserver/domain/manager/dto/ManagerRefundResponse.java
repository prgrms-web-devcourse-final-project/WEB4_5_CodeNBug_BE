package org.codeNbug.mainserver.domain.manager.dto;

import lombok.*;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ManagerRefundResponse {
    private Long purchaseId;
    private Long userId;
    private PaymentStatusEnum paymentStatus;
    private List<Long> ticketId;
    private Integer refundAmount;
    private LocalDateTime refundDate;
}
