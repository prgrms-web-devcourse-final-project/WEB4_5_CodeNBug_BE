package org.codeNbug.mainserver.domain.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TicketDto {
    private Long purchaseId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String phoneNum;
    private PaymentStatusEnum payment_status;
    private LocalDateTime purchaseAt;
    private Integer amount;
    private Long ticket_id;
}
