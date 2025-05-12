package org.codeNbug.mainserver.domain.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대시보드 통계 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    
    /**
     * 총 사용자 수 (일반 + SNS)
     */
    private long totalUsers;
    
    /**
     * 총 이벤트 수
     */
    private long totalEvents;
    
    /**
     * 총 티켓 수 (현재는 사용하지 않음)
     */
    private long totalTickets;
} 