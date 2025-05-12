package org.codeNbug.mainserver.domain.seat.dto;

import java.util.Comparator;
import java.util.List;

import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class SeatLayoutResponse {

	private final List<SeatDto> seats;
	private final List<List<String>> layout;

	@Getter
	@NoArgsConstructor
	public static class SeatDto {
		private Long seatId;
		private String location;
		private String grade;
		private boolean available;

		public SeatDto(Long seatId, String location, String grade, boolean available) {
			this.seatId = seatId;
			this.location = location;
			this.grade = grade;
			this.available = available;
		}
	}

	public SeatLayoutResponse(List<Seat> seatList, SeatLayout seatLayout) {
		this.seats = seatList.stream()
			.sorted(Comparator.comparing(Seat::getId))
			.map(seat -> new SeatDto(
				seat.getId(),
				seat.getLocation(),
				seat.getGrade().getGrade().name(),
				seat.isAvailable()
			))
			.toList();

		this.layout = extractLayoutFromJson(seatLayout.getLayout());
	}

	private List<List<String>> extractLayoutFromJson(String layoutJson) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(layoutJson);
			JsonNode layoutNode = root.get("layout");

			if (layoutNode == null || !layoutNode.isArray()) {
				throw new IllegalArgumentException("layout 필드가 존재하지 않거나 배열이 아닙니다.");
			}

			return mapper.readValue(
				layoutNode.traverse(),
				new TypeReference<>() {
				}
			);
		} catch (Exception e) {
			throw new RuntimeException("좌석 레이아웃 JSON 파싱 실패", e);
		}
	}
}