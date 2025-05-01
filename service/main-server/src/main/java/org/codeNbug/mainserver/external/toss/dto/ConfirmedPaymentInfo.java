package org.codeNbug.mainserver.external.toss.dto;

import java.time.LocalDateTime;

import org.codeNbug.mainserver.domain.purchase.entity.PaymentMethodEnum;

import lombok.Getter;

/**
 * Toss 결제 승인 응답을 담는 DTO (TOSS API 응답)
 * Toss API 문서에 명시된 필드를 기반으로 매핑
 */
@Getter
public class ConfirmedPaymentInfo {
	private String paymentUuid;    // Toss 결제 키
	private String orderId;       // 우리가 요청한 주문 ID
	private String orderName;     // 주문 이름
	private Integer totalAmount;   // 결제 금액
	private String status;        // 결제 상태 (DONE, CANCELED 등)
	private PaymentMethodEnum method;        // 결제 수단 (CARD 등)
	private LocalDateTime approvedAt;    // 결제 승인 일시
}
