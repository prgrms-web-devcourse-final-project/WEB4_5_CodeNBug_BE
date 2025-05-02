package org.codeNbug.mainserver.domain.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Data
@AllArgsConstructor
public class EventPurchaseResponse {
    private Long purchaseId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String phoneNum;
    private PaymentStatusEnum payment_status;
    private List<Long> ticket_id;
    private LocalDateTime purchaseAt;
    private Integer amount;
}
