package org.codeNbug.mainserver.domain.manager.dto.layout;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDto {
    private String grade; // 좌석 등급 (ex: S, A 등)
    private int amount;   // 가격 (ex: 50000)
}
