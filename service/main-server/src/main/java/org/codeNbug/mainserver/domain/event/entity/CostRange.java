package org.codeNbug.mainserver.domain.event.entity;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CostRange {

	public static final CostRange from0to100_00 = new CostRange(0, 100_00);
	public static final CostRange from100_01to500_00 = new CostRange(100_01, 500_00);
	public static final CostRange from500_01to1000_00 = new CostRange(500_01, 1000_00);

	@Min(0)
	private Integer min = 0;
	@Min(0)
	private Integer max = 0;

	public CostRange(Integer min, Integer max) {
		if (min > max)
			throw new IllegalArgumentException("min should be less than max");
		this.min = min;
		this.max = max;

	}

	public Integer getMin() {
		return min;
	}

	public Integer getMax() {
		return max;
	}
}
