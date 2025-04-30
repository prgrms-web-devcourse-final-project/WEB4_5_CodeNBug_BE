package org.codeNbug.mainserver.domain.seat.dto;

import java.util.Comparator;
import java.util.List;

import org.codeNbug.mainserver.domain.seat.entity.Seat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class SeatLayoutResponse {

	private List<SeatDto> seats;

	@Getter
	@NoArgsConstructor
	public static class SeatDto {
		private String location;
		private String grade;
		private boolean available;

		public SeatDto(String location, String grade, boolean available) {
			this.location = location;
			this.grade = grade;
			this.available = available;
		}
	}

	public SeatLayoutResponse(List<Seat> seatList) {
		this.seats = seatList.stream()
				.sorted(Comparator.comparing(Seat::getId))  // seatId 순 정렬 추가
				.map(seat -> new SeatDto(
						seat.getLocation(),
						seat.getGrade().getGrade().name(),
						seat.isAvailable()
				))
				.toList();
	}

}