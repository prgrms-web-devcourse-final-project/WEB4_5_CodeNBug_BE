package org.codeNbug.mainserver.domain.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CancelPaymentRequest {
	private String cancelReason;
}