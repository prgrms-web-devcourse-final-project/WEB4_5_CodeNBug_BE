package org.codeNbug.mainserver.domain.admin.dto.response;

import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAdminDto {
    private Long ticketId;
    private Long eventId;
    private String eventTitle;
    private String userName;
    private String userEmail;
    private String phoneNumber;
    private String seatInfo;
    private PaymentStatusEnum paymentStatus;
    private Integer amount;
    private LocalDateTime purchaseDate;
} 