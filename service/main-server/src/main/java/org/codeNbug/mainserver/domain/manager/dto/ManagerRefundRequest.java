package org.codeNbug.mainserver.domain.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerRefundRequest {
    private List<Long> purchasesIds;
    private boolean totalRefund;
    private String reason;
}
