package org.codeNbug.mainserver.domain.manager.dto.layout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatInfoDto {
    private String grade; // 예: "S", "A" 등급
}
