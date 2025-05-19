package org.codeNbug.mainserver.domain.admin.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여러 사용자 ID를 일괄 처리하기 위한 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserIdsRequest {
    
    @NotEmpty(message = "처리할 사용자 ID 목록은 비어있을 수 없습니다")
    private List<Long> userIds;
} 