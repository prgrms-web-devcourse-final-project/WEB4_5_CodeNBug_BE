package org.codeNbug.mainserver.domain.event.entity;

public class CostRange {

	public static final CostRange from0to100_00 = new CostRange(0, 100_00);
	public static final CostRange from100_01to500_00 = new CostRange(100_01, 500_00);
	public static final CostRange from500_01to1000_00 = new CostRange(500_01, 1000_00);

	private final Integer min;
	private final Integer max;

	public CostRange(Integer min, Integer max) {
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
