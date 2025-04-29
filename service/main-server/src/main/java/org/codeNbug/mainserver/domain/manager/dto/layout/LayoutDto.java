package org.codeNbug.mainserver.domain.manager.dto.layout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LayoutDto {
    private List<List<String>> layout; // 2차원 좌석 배치 (ex: A1, A2, B1...)
    private Map<String, SeatInfoDto> seat; // 좌석 이름 -> 좌석 정보
}
