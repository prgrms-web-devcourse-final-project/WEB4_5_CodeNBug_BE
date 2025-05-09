package org.codeNbug.mainserver.domain.seat.entity;

/**
 * SeatGrade Enum 클래스
 */
public enum SeatGradeEnum {
	VIP,
	R,
	S,
	A,
	B,
	STANDING;

	public static SeatGradeEnum fromString(String grade) {
		switch (grade) {
			case "VIP":
				return VIP;
			case "R":
				return R;
			case "S":
				return S;
			case "A":
				return A;
			case "B":
				return B;
			case "STANDING":
				return STANDING;
			default:
				throw new IllegalArgumentException("Unknown grade: " + grade);
		}
	}
}
